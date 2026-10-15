all: build deploy start
.PHONY: build deploy

build:
	./gradlew clean war -Pvaadin.productionMode

deploy:
	cp build/libs/is-lab-1.0-SNAPSHOT.war /home/astrosoup/wildfly-41.0.1.Final/standalone/deployments/ROOT.war

start:
	/home/astrosoup/wildfly-41.0.1.Final/bin/standalone.sh

undeploy:
	rm /home/astrosoup/wildfly-41.0.1.Final/standalone/deployments/ROOT.war

deploy-helios:
	scp build/libs/is-lab-1.0-SNAPSHOT.war helios:wildfly-41.0.1.Final/standalone/deployments/ROOT.war
