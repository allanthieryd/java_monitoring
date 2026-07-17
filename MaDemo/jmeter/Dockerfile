FROM eclipse-temurin:21-jre-alpine

ARG JMETER_VERSION=5.6.3

# Apache JMeter embarque Groovy dans ses lib/ : les elements JSR223 des plans
# de test fonctionnent sans dependance supplementaire.
RUN apk add --no-cache curl \
    && curl -fsSL "https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz" \
       -o /tmp/jmeter.tgz \
    && mkdir -p /opt/jmeter \
    && tar -xzf /tmp/jmeter.tgz -C /opt/jmeter --strip-components=1 \
    && rm /tmp/jmeter.tgz

ENV PATH="/opt/jmeter/bin:${PATH}"

# Heap du JVM JMeter. A monter si le plan simule beaucoup de threads.
ENV JVM_ARGS="-Xms512m -Xmx1g"

COPY run.sh /usr/local/bin/run.sh
RUN chmod +x /usr/local/bin/run.sh

WORKDIR /jmeter

ENTRYPOINT ["/usr/local/bin/run.sh"]
