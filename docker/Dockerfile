FROM eclipse-temurin:17-jdk

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV CARGO_HOME=/opt/cargo
ENV RUSTUP_HOME=/opt/rustup
ENV PATH="${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${CARGO_HOME}/bin"

RUN apt-get update && apt-get install -y --no-install-recommends \
    wget unzip git curl build-essential cmake ninja-build \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

RUN yes | sdkmanager --licenses >/dev/null 2>&1 || true
RUN sdkmanager --install \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;36.0.0" \
    "ndk;29.0.14206865" \
    "cmake;4.1.2"

RUN wget -q https://sh.rustup.rs -O /tmp/rustup-init.sh \
    && chmod +x /tmp/rustup-init.sh \
    && /tmp/rustup-init.sh -y --default-toolchain 1.93.0 --profile minimal \
    && rm /tmp/rustup-init.sh
RUN . ${CARGO_HOME}/env \
    && rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android

# Container runs as root; the bind-mounted /workspace is owned by the host user, and
# app/build.gradle.kts shells out to `git` to embed BUILD_COMMIT_HASH. Without this,
# git refuses to run ("detected dubious ownership in repository") and the build fails
# at that line with exit 128.
RUN git config --global --add safe.directory /workspace

WORKDIR /workspace
CMD ["./gradlew", "assembleDebug", "--no-daemon"]
