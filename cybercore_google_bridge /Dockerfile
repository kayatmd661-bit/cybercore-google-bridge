ARG BUILD_FROM=ghcr.io/home-assistant/amd64-base:latest
FROM $BUILD_FROM

# সিস্টেম এনভায়রনমেন্ট সেটআপ
ENV LANG=C.UTF-8

# পাইথন এবং প্রয়োজনীয় বিল্ড টুলস ইন্সটল করা
RUN apk add --no-cache \
    python3 \
    py3-pip \
    py3-setuptools \
    gcc \
    musl-dev \
    python3-dev

# কাজের জন্য ডিরেক্টরি তৈরি
WORKDIR /app

# ডিপেন্ডেন্সি ফাইল ও মেইন কোড কন্টেইনারে কপি করা
COPY requirements.txt /app/
COPY bridge_agent.py /app/

# পাইথন প্যাকেজগুলো ইন্সটল করা
RUN pip3 install --no-cache-dir -r requirements.txt --break-system-packages

# এনভায়রনমেন্ট ভ্যারিয়েবল পাস করার স্ক্রিপ্ট (হোম অ্যাসিস্ট্যান্ট অপশন রিড করার জন্য)
CMD [ "python3", "/app/bridge_agent.py" ]
