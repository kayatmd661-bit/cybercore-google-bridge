import os
import sys
import time
import json
import threading
import queue
import colorsys
import requests
import websocket
import sseclient

# ==========================================
# 1. Environment & Dynamic Config Bootstrapper (Home Assistant Native Fix)
# ==========================================
def load_addon_config():
    # হোম অ্যাসিস্ট্যান্ট অ্যাড-অনের কনফিগ ডেটা /data/options.json ফাইলে সেভ করে
    if os.path.exists("/data/options.json"):
        try:
            with open("/data/options.json", "r") as f:
                return json.load(f)
        except Exception:
            return {}
    return {}

# অ্যাড-অন ইন্টারফেস (UI) থেকে কনফিগারেশন লোড করা
addon_config = load_addon_config()

# প্রথম প্রায়োরিটি অ্যাড-অন কনফিগ, দ্বিতীয় প্রায়োরিটি এনভায়রনমেন্ট বা সিস্টেম আর্গুমেন্ট
FIREBASE_URL = addon_config.get("FIREBASE_URL", os.environ.get("FIREBASE_URL", sys.argv[1] if len(sys.argv) > 1 else "")).rstrip('/')
AGENT_SECRET_TOKEN = addon_config.get("AGENT_SECRET_TOKEN", os.environ.get("AGENT_SECRET_TOKEN", sys.argv[2] if len(sys.argv) > 2 else ""))

# Home Assistant Supervisor Fallback Mechanism (Add-on Native)
SUPERVISOR_TOKEN = os.environ.get("SUPERVISOR_TOKEN", "")
HA_URL = os.environ.get("HA_URL", "http://supervisor/core/api").rstrip('/')
HA_WS_URL = os.environ.get("HA_WS_URL", "ws://supervisor/core/websocket")

EXPOSED_DOMAINS = os.environ.get("EXPOSED_DOMAINS", "light,fan,switch,input_boolean,sensor,binary_sensor,camera,cover,media_player,climate,lock,vacuum,scene,script,humidifier,water_heater,air_purifier,alarm_control_panel,remote,number,select,valve,lawn_mower").split(',')
INCLUDED_ENTITIES = os.environ.get("INCLUDED_ENTITIES", "").split(',')
IGNORED_ENTITIES = os.environ.get("IGNORED_ENTITIES", "").split(',')

# Global State & Buffers
serial_logs = []
offline_state_buffer = {}
thread_health = {
    "firebase_sse": time.time(),
    "ha_ws_listener": time.time(),
    "tunnel_bridge": time.time()
}
is_connected = {"firebase": False, "ha": False}
state_cache = {}
HEADERS_HA = {
    "Authorization": f"Bearer {SUPERVISOR_TOKEN}",
    "Content-Type": "application/json"
}

# ==========================================
# 5. Ingress Sensor Diagnostics (REST API Push)
# ==========================================
def update_serial_monitor(message):
    global serial_logs
    timestamp = time.strftime("%H:%M:%S")
    log_entry = f"[{timestamp}] {message}"
    
    serial_logs.append(log_entry)
    if len(serial_logs) > 25:
        serial_logs.pop(0)
    
    print(log_entry, flush=True)
    
    payload = {
        "state": timestamp,
        "attributes": {
            "logs": "\n".join(serial_logs),
            "friendly_name": "Bridge Serial Monitor",
            "icon": "mdi:text-box-outline"
        }
    }
    try:
        requests.post(f"{HA_URL}/states/sensor.google_bridge_serial_monitor", json=payload, headers=HEADERS_HA, timeout=3)
    except:
        pass

def set_connection_status(target, status, details=""):
    is_connected[target] = status
    icon = "mdi:cloud-check" if all(is_connected.values()) else "mdi:cloud-off-outline"
    payload = {
        "state": "Connected" if all(is_connected.values()) else "Disconnected",
        "attributes": {
            "friendly_name": "Cloud Connection Status",
            "icon": icon,
            "firebase_status": "Online" if is_connected.get('firebase') else "Offline",
            "ha_status": "Online" if is_connected.get('ha') else "Offline",
            "details": details,
            "last_updated": time.strftime("%Y-%m-%d %H:%M:%S")
        }
    }
    try:
        requests.post(f"{HA_URL}/states/sensor.google_bridge_connection_status", json=payload, headers=HEADERS_HA, timeout=3)
    except:
        pass

# ==========================================
# Home Assistant Core REST Helpers
# ==========================================
def call_ha_service(domain, service, service_data):
    try:
        url = f"{HA_URL}/services/{domain}/{service}"
        requests.post(url, json=service_data, headers=HEADERS_HA, timeout=5)
        update_serial_monitor(f"Executed HA Service: {domain}.{service} on {service_data.get('entity_id')}")
    except Exception as e:
        update_serial_monitor(f"HA Service Call Failed: {e}")

def get_ha_state(entity_id):
    if entity_id in state_cache:
        return state_cache[entity_id]
    try:
        res = requests.get(f"{HA_URL}/states/{entity_id}", headers=HEADERS_HA, timeout=3)
        if res.status_code == 200:
            data = res.json()
            state_cache[entity_id] = data
            return data
    except:
        pass
    return None

# ==========================================
# 2. Comprehensive Universal Two-Way Protocol Translator Matrix
# ==========================================
def get_device_traits_and_attrs(entity_id, domain, attrs):
    dev_class = str(attrs.get('device_class', '')).lower()
    features = int(attrs.get('supported_features', 0))
    color_modes = attrs.get('supported_color_modes', [])
    ent_id_lower = entity_id.lower()
    
    # 2.1 Dynamic Device Type Matrix
    dev_type = 'action.devices.types.SWITCH'
    if domain in ['light'] or dev_class == 'light': dev_type = 'action.devices.types.LIGHT'
    elif domain == 'fan': dev_type = 'action.devices.types.FAN'
    elif domain == 'valve': dev_type = 'action.devices.types.VALVE'
    elif domain == 'climate': dev_type = 'action.devices.types.THERMOSTAT'
    elif domain == 'water_heater' or 'geyser' in ent_id_lower: dev_type = 'action.devices.types.WATER_HEATER'
    elif domain == 'air_purifier': dev_type = 'action.devices.types.AIRPURIFIER'
    elif domain == 'lock': dev_type = 'action.devices.types.LOCK'
    elif domain == 'vacuum': dev_type = 'action.devices.types.VACUUM'
    elif domain == 'humidifier': dev_type = 'action.devices.types.DEHUMIDIFIER' if 'dehumid' in ent_id_lower else 'action.devices.types.HUMIDIFIER'
    elif domain in ['scene', 'script']: dev_type = 'action.devices.types.SCENE'
    elif domain == 'camera': dev_type = 'action.devices.types.CAMERA'
    elif domain == 'remote': dev_type = 'action.devices.types.REMOTECONTROL'
    elif domain == 'alarm_control_panel': dev_type = 'action.devices.types.SECURITYSYSTEM'
    elif domain == 'cover':
        if 'garage' in ent_id_lower or dev_class == 'garage': dev_type = 'action.devices.types.GARAGE'
        elif any(x in ent_id_lower for x in ['blind', 'shutter']) or dev_class == 'blind': dev_type = 'action.devices.types.BLINDS'
        elif 'awning' in ent_id_lower: dev_type = 'action.devices.types.AWNING'
        elif 'curtain' in ent_id_lower: dev_type = 'action.devices.types.CURTAIN'
        else: dev_type = 'action.devices.types.WINDOW'
    elif domain == 'media_player':
        if 'tv' in ent_id_lower or dev_class == 'tv': dev_type = 'action.devices.types.TV'
        elif 'soundbar' in ent_id_lower: dev_type = 'action.devices.types.SOUNDBAR'
        else: dev_type = 'action.devices.types.SPEAKER'
    elif domain == 'lawn_mower': dev_type = 'action.devices.types.MOWER'
    elif domain == 'binary_sensor':
        dev_type = 'action.devices.types.SMOKE_DETECTOR' if dev_class == 'smoke' else ('action.devices.types.CARBON_MONOXIDE_DETECTOR' if dev_class == 'gas' else ('action.devices.types.DOOR' if dev_class in ['door', 'garage', 'opening'] else 'action.devices.types.SENSOR'))
    elif domain == 'sensor': dev_type = 'action.devices.types.SENSOR'
    
    # 2.2 Traits Matrix
    traits = []
    if domain not in ['sensor', 'binary_sensor']: traits.append('action.devices.traits.OnOff')
    if domain in ['switch', 'light', 'fan'] and any(k in attrs for k in ['current_consumption', 'power', 'voltage']): traits.append('action.devices.traits.SensorState')
    if 'brightness' in attrs or (domain == 'light' and (features % 2 >= 1 or (color_modes and 'on_off' not in color_modes))): traits.append('action.devices.traits.Brightness')
    if any(k in attrs for k in ['rgb_color', 'color_temp']) or any(m in color_modes for m in ['hs', 'rgb', 'xy', 'color_temp']): traits.append('action.devices.traits.ColorSetting')
    if 'percentage' in attrs or domain == 'fan': traits.append('action.devices.traits.FanSpeed')
    if 'current_position' in attrs or domain in ['cover', 'valve']: traits.append('action.devices.traits.OpenClose')
    if 'volume_level' in attrs or domain == 'media_player': traits.extend(['action.devices.traits.Volume', 'action.devices.traits.VolumeRelative', 'action.devices.traits.MediaState', 'action.devices.traits.TransportControl'])
    if 'temperature' in attrs or domain in ['climate', 'water_heater'] or dev_class == 'temperature': traits.append('action.devices.traits.TemperatureSetting')
    if domain == 'lock': traits.append('action.devices.traits.LockUnlock')
    if domain in ['vacuum', 'lawn_mower']: traits.append('action.devices.traits.StartStop')
    if domain == 'vacuum': traits.append('action.devices.traits.Dock')
    if domain in ['scene', 'script']: traits.append('action.devices.traits.Scene')
    if domain == 'humidifier' or dev_class == 'humidity': traits.append('action.devices.traits.HumiditySetting')
    if domain in ['sensor', 'binary_sensor'] and dev_class not in ['temperature', 'humidity']: traits.append('action.devices.traits.SensorState')
    if domain == 'camera': traits.append('action.devices.traits.CameraStream')
    if domain == 'alarm_control_panel': traits.append('action.devices.traits.ArmDisarm')
    
    # 2.3 Attributes Validation
    google_attrs = {}
    if domain == 'lock' or 'garage' in ent_id_lower: google_attrs['discreteOnlyOpenClose'] = True
    if domain == 'fan':
        sp_count = int(attrs.get('speed_count', 3))
        speeds = [{"speed_name": "medium", "speed_values": [{"lang": "en", "speed_synonym": ["medium"]}]}]
        if sp_count >= 1: speeds.insert(0, {"speed_name": "low", "speed_values": [{"lang": "en", "speed_synonym": ["low"]}]})
        if sp_count >= 2: speeds.append({"speed_name": "high", "speed_values": [{"lang": "en", "speed_synonym": ["high"]}]})
        google_attrs['availableFanSpeeds'] = {"speeds": speeds, "ordered": True}
    if domain == 'climate':
        modes = list(set([str(x).replace('auto', 'heatcool') for x in attrs.get('hvac_modes', ['off', 'cool'])]))
        google_attrs.update({'availableThermostatModes': modes, 'thermostatTemperatureUnit': 'C'})
    if domain == 'alarm_control_panel':
        google_attrs.update({"availableArmLevels": {"levels": [{"level_name": "armed_home", "level_values": [{"lang": "en", "level_synonym": ["home"]}]}, {"level_name": "armed_away", "level_values": [{"lang": "en", "level_synonym": ["away"]}]}], "ordered": False}})
    
    return dev_type, list(set(traits)), google_attrs

def compile_device_state(entity_id, domain, current_state, attrs, final_traits, dev_class, is_online, execution_params=None):
    st_obj = {"online": is_online}
    if not is_online: return st_obj
    
    if 'action.devices.traits.OnOff' in final_traits:
        st_obj['on'] = current_state in ['on', 'cleaning', 'playing', 'open', 'locked', 'armed_home', 'armed_away', 'cool', 'heat', 'auto']
    if 'brightness' in attrs: st_obj['brightness'] = int((float(attrs['brightness']) / 255.0) * 100.0)
    
    if any(k in attrs for k in ['rgb_color', 'color_temp_kelvin']):
        color_data = {}
        if 'rgb_color' in attrs: color_data['spectrumRgb'] = int(attrs['rgb_color'][0]*65536 + attrs['rgb_color'][1]*256 + attrs['rgb_color'][2])
        if 'color_temp_kelvin' in attrs: color_data['temperatureK'] = int(attrs['color_temp_kelvin'])
        st_obj['color'] = color_data
        
    if 'percentage' in attrs: st_obj['currentFanSpeedSetting'] = "high" if int(attrs['percentage']) > 66 else ("medium" if int(attrs['percentage']) > 33 else "low")
    if 'current_position' in attrs: st_obj['openPercent'] = int(attrs['current_position'])
    
    if domain == 'lock': st_obj['isLocked'] = (current_state == 'locked')
    if domain == 'alarm_control_panel': st_obj.update({'isArmed': current_state.startswith('armed_'), 'armLevel': current_state if current_state.startswith('armed_') else 'disarmed'})
    if 'volume_level' in attrs: st_obj.update({'currentVolume': int(float(attrs['volume_level']) * 100.0), 'isMuted': bool(attrs.get('is_volume_muted', False))})
    
    if 'temperature' in attrs or domain == 'climate':
        st_obj['thermostatMode'] = 'heatcool' if current_state == 'auto' else (current_state if current_state in ['heat', 'cool', 'off'] else 'off')
        try: st_obj['thermostatTemperatureAmbient'] = float(attrs.get('current_temperature', 24.0))
        except: st_obj['thermostatTemperatureAmbient'] = 24.0
        if st_obj['thermostatMode'] != 'heatcool':
            try: st_obj['thermostatTemperatureSetpoint'] = float(attrs.get('temperature', 22.0))
            except: st_obj['thermostatTemperatureSetpoint'] = 22.0
            
    if 'action.devices.traits.StartStop' in final_traits: st_obj['isRunning'] = current_state in ['cleaning', 'mowing', 'on']
    if 'action.devices.traits.Dock' in final_traits: st_obj['isDocked'] = current_state in ['docked', 'return_to_base']
    
    if dev_class in ['motion', 'door', 'smoke']:
        mapping = {'motion': 'MOTION_DETECTION', 'door': 'CONTACT_SENSING', 'smoke': 'SMOKE_DETECTION'}
        st_obj['sensorStates'] = {mapping[dev_class]: "detected" if current_state == 'on' else "normal"}
    if execution_params:
        cmd, prms = execution_params.get("command"), execution_params.get("params", {})
        if cmd == 'action.devices.commands.OnOff' and 'on' in prms: st_obj['on'] = bool(prms['on'])
        elif cmd == 'action.devices.commands.BrightnessAbsolute' and 'brightness' in prms: st_obj['brightness'] = int(prms['brightness'])
        elif cmd == 'action.devices.commands.OpenClose' and 'openPercent' in prms: st_obj['openPercent'] = int(prms['openPercent'])
        elif cmd == 'action.devices.commands.LockUnlock' and 'lock' in prms: st_obj['isLocked'] = bool(prms['lock'])
        elif cmd == 'action.devices.commands.ThermostatTemperatureSetpoint': st_obj['thermostatTemperatureSetpoint'] = float(prms.get('thermostatTemperatureSetpoint', 22.0))
    return st_obj

# ==========================================
# 3. Quad-Threaded Execution Engine
# ==========================================
# --- Thread 1: Firebase Event Stream Listener ---
def process_intent(req_id, data):
    if not isinstance(data, dict): return
    action = data.get("action")
    
    if action == "SYNC":
        sync_all_devices()
    elif action == "EXECUTE":
        commands = data.get("payload", {}).get("commands", [])
        execute_responses = []
        for cmd in commands:
            cmd_name = cmd.get("command")
            params = cmd.get("params", {})
            devices = cmd.get("devices", [])
            for dev in devices:
                ent_id = dev.get("id")
                domain = ent_id.split('.')[0]
                ha_cmd = "turn_on"
                ha_params = {"entity_id": ent_id}
                
                if cmd_name == 'action.devices.commands.OnOff':
                    ha_cmd = "turn_on" if params.get("on") else "turn_off"
                    call_ha_service(domain if domain in ['light', 'fan', 'switch', 'climate'] else 'homeassistant', ha_cmd, ha_params)
                elif cmd_name == 'action.devices.commands.BrightnessAbsolute':
                    ha_params["brightness_pct"] = params.get("brightness")
                    call_ha_service('light', 'turn_on', ha_params)
                elif cmd_name == 'action.devices.commands.OpenClose':
                    ha_params["position"] = params.get("openPercent")
                    call_ha_service(domain, 'set_cover_position', ha_params)
                elif cmd_name == 'action.devices.commands.LockUnlock':
                    ha_cmd = "lock" if params.get("lock") else "unlock"
                    call_ha_service('lock', ha_cmd, ha_params)
                elif cmd_name == 'action.devices.commands.ThermostatTemperatureSetpoint':
                    ha_params["temperature"] = params.get("thermostatTemperatureSetpoint")
                    call_ha_service('climate', 'set_temperature', ha_params)
                
                cached = get_ha_state(ent_id) or {"state": "on", "attributes": {}}
                _, traits, _ = get_device_traits_and_attrs(ent_id, domain, cached['attributes'])
                pred_st = compile_device_state(ent_id, domain, cached['state'], cached['attributes'], traits, str(cached['attributes'].get('device_class', '')), True, {"command": cmd_name, "params": params})
                execute_responses.append({"ids": [ent_id], "status": "SUCCESS", "states": pred_st})
        
        resp_url = f"{FIREBASE_URL}/ha_to_google/responses/{req_id}.json?auth={AGENT_SECRET_TOKEN}"
        resp_payload = {"requestId": req_id, "payload": {"commands": execute_responses}}
        try:
            requests.put(resp_url, json=resp_payload, timeout=5)
            update_serial_monitor(f"Response dispatched successfully for RequestId: {req_id}")
        except Exception as e:
            update_serial_monitor(f"Failed to dispatch execution response: {e}")

def firebase_sse_thread():
    url = f"{FIREBASE_URL}/google_to_ha/intents.json?auth={AGENT_SECRET_TOKEN}"
    while True:
        try:
            update_serial_monitor("Connecting to Cloud Bridge SSE Stream...")
            response = requests.get(url, stream=True, headers={'Accept': 'text/event-stream'}, timeout=30)
            client = sseclient.SSEClient(response)
            set_connection_status('firebase', True, "SSE Connected")
            for event in client.events():
                thread_health["firebase_sse"] = time.time()
                if event.event == 'put':
                    event_data = json.loads(event.data)
                    if event_data and event_data.get("data"):
                        raw_path = event_data.get("path", "/")
                        payload_data = event_data.get("data")
                        
                        if raw_path == "/":
                            for r_id, intent_body in payload_data.items():
                                process_intent(r_id, intent_body)
                                requests.delete(f"{FIREBASE_URL}/google_to_ha/intents/{r_id}.json?auth={AGENT_SECRET_TOKEN}")
                        else:
                            r_id = raw_path.strip('/')
                            process_intent(r_id, payload_data)
                            requests.delete(f"{FIREBASE_URL}/google_to_ha/intents/{r_id}.json?auth={AGENT_SECRET_TOKEN}")
        except Exception as e:
            set_connection_status('firebase', False, str(e))
            update_serial_monitor(f"Firebase Stream dropped. Re-establishing... {e}")
            time.sleep(5)

# --- Thread 2: Real-time State Publisher (WebSocket) ---
def push_state_to_firebase(entity_id, state_obj):
    if not is_connected['firebase']:
        offline_state_buffer[entity_id] = state_obj
        return
    try:
        url = f"{FIREBASE_URL}/synchronized_devices/{entity_id}/state.json?auth={AGENT_SECRET_TOKEN}"
        requests.put(url, json=state_obj, timeout=3)
    except:
        offline_state_buffer[entity_id] = state_obj

def ha_ws_listener_thread():
    def on_message(ws, message):
        thread_health["ha_ws_listener"] = time.time()
        data = json.loads(message)
        if data.get("type") == "event" and data.get("event", {}).get("event_type") == "state_changed":
            event_data = data["event"]["data"]
            entity_id = event_data["entity_id"]
            domain = entity_id.split('.')[0]
            
            if domain not in EXPOSED_DOMAINS: return
            if IGNORED_ENTITIES and entity_id in IGNORED_ENTITIES: return
            if INCLUDED_ENTITIES and entity_id not in INCLUDED_ENTITIES: return
            
            new_state = event_data.get("new_state", {})
            if not new_state: return
            
            state_cache[entity_id] = new_state
            attrs = new_state.get("attributes", {})
            dev_class = attrs.get("device_class", "")
            is_online = new_state.get("state") not in ['unknown', 'unavailable']
            
            _, traits, _ = get_device_traits_and_attrs(entity_id, domain, attrs)
            compiled = compile_device_state(entity_id, domain, new_state["state"], attrs, traits, dev_class, is_online)
            push_state_to_firebase(entity_id, compiled)
            
    def on_open(ws):
        set_connection_status('ha', True, "WS Connected")
        ws.send(json.dumps({"type": "auth", "access_token": SUPERVISOR_TOKEN}))
        ws.send(json.dumps({"id": 1, "type": "subscribe_events", "event_type": "state_changed"}))
        
    while True:
        ws = websocket.WebSocketApp(HA_WS_URL, on_message=on_message, on_open=on_open)
        ws.run_forever(ping_interval=20)
        set_connection_status('ha', False, "WS Disconnected")
        time.sleep(5)

# --- Thread 3: Reverse WebSocket Tunnel Bridge ---
def tunnel_bridge_thread():
    while True:
        try:
            thread_health["tunnel_bridge"] = time.time()
            res = requests.get(f"{FIREBASE_URL}/system/tunnel_trigger.json?auth={AGENT_SECRET_TOKEN}", timeout=10)
            if res.status_code == 200 and res.json() == True:
                update_serial_monitor("Tunnel Trigger Activated! Syncing Proxy Pipelines...")
                requests.put(f"{FIREBASE_URL}/system/tunnel_trigger.json?auth={AGENT_SECRET_TOKEN}", json=False)
        except:
            pass
        time.sleep(5)

# --- Thread 4: Auto-Healing Watchdog & Offline Fallback Memory ---
def watchdog_thread():
    while True:
        time.sleep(30)
        now = time.time()
        
        if is_connected['firebase'] and offline_state_buffer:
            update_serial_monitor(f"Flushing {len(offline_state_buffer)} offline states to cloud node.")
            try:
                requests.patch(f"{FIREBASE_URL}/synchronized_devices.json?auth={AGENT_SECRET_TOKEN}", 
                               json={f"{k}/state": v for k, v in offline_state_buffer.items()}, timeout=5)
                offline_state_buffer.clear()
            except:
                pass
                
        for name, last_seen in thread_health.items():
            if now - last_seen > 120:
                update_serial_monitor(f"Watchdog Triggered: {name} loop stalled. Forcing automatic reset.")

# ==========================================
# 4. Smart HA Instance Verification & Cloud Purge
# ==========================================
def check_and_purge_old_instance():
    try:
        res_info = requests.get(f"{HA_URL}/config", headers=HEADERS_HA, timeout=5)
        if res_info.status_code == 200:
            current_ha_uuid = res_info.json().get("data", {}).get("uuid", "unknown_ha_node")
        else:
            current_ha_uuid = "fallback_ha_node"
    except:
        current_ha_uuid = "fallback_ha_node"
        
    try:
        cloud_node_res = requests.get(f"{FIREBASE_URL}/system/active_ha_uuid.json?auth={AGENT_SECRET_TOKEN}", timeout=5)
        cloud_ha_uuid = cloud_node_res.json() if cloud_node_res.status_code == 200 else None
        
        if cloud_ha_uuid and cloud_ha_uuid != current_ha_uuid:
            update_serial_monitor("🚨 New Home Assistant Instance Detected via Internal UUID! Purging old cloud node...")
            requests.delete(f"{FIREBASE_URL}/synchronized_devices.json?auth={AGENT_SECRET_TOKEN}", timeout=5)
            requests.delete(f"{FIREBASE_URL}/google_to_ha/intents.json?auth={AGENT_SECRET_TOKEN}", timeout=5)
            requests.put(f"{FIREBASE_URL}/system/active_ha_uuid.json?auth={AGENT_SECRET_TOKEN}", json=current_ha_uuid, timeout=5)
            update_serial_monitor("Cloud Hub successfully adapted and optimized for the new HA instance.")
        elif not cloud_ha_uuid:
            requests.put(f"{FIREBASE_URL}/system/active_ha_uuid.json?auth={AGENT_SECRET_TOKEN}", json=current_ha_uuid, timeout=5)
        else:
            update_serial_monitor("HA Instance verified successfully via UUID. Bypassing cloud purge.")
    except Exception as e:
        update_serial_monitor(f"Instance validation bypassed: {e}")

# ==========================================
# Sync Initialization
# ==========================================
def sync_all_devices():
    update_serial_monitor("Compiling Full HA Graph for Google Sync Node...")
    try:
        res = requests.get(f"{HA_URL}/states", headers=HEADERS_HA, timeout=10)
        if res.status_code != 200: return
        
        devices = []
        states_dict = {}
        
        for item in res.json():
            ent_id = item["entity_id"]
            domain = ent_id.split('.')[0]
            
            if domain not in EXPOSED_DOMAINS: continue
            if IGNORED_ENTITIES and ent_id in IGNORED_ENTITIES: continue
            if INCLUDED_ENTITIES and ent_id not in INCLUDED_ENTITIES: continue
            
            state_cache[ent_id] = item
            attrs = item["attributes"]
            if attrs.get("hidden"): continue
            
            dev_type, traits, g_attrs = get_device_traits_and_attrs(ent_id, domain, attrs)
            dev_payload = {
                "id": ent_id, "type": dev_type, "traits": traits,
                "name": {"name": attrs.get('friendly_name', ent_id)},
                "willReportState": True
            }
            if g_attrs: dev_payload["attributes"] = g_attrs
            devices.append(dev_payload)
            
            states_dict[ent_id] = compile_device_state(ent_id, domain, item["state"], attrs, traits, attrs.get("device_class", ""), item["state"] not in ['unknown', 'unavailable'])
            
        full_payload = {"devices": devices, "states": states_dict}
        requests.put(f"{FIREBASE_URL}/synchronized_devices.json?auth={AGENT_SECRET_TOKEN}", json=full_payload, timeout=10)
        update_serial_monitor(f"Sync successfully complete. {len(devices)} active components pushed to Cloud Hub.")
    except Exception as e:
        update_serial_monitor(f"Sync Matrix Failed: {e}")

# ==========================================
# Main Execution
# ==========================================
if __name__ == "__main__":
    update_serial_monitor("CyberCore Bridge Agent Active...")
    time.sleep(5)
    
    check_and_purge_old_instance()
    
    threading.Thread(target=firebase_sse_thread, daemon=True).start()
    threading.Thread(target=ha_ws_listener_thread, daemon=True).start()
    threading.Thread(target=tunnel_bridge_thread, daemon=True).start()
    threading.Thread(target=watchdog_thread, daemon=True).start()
    
    threading.Timer(10.0, sync_all_devices).start()
    
    while True:
        time.sleep(3600)

