JC=javac

targets:
	$(JC) com/ds18749/component/*.java

.PHONY: runserver runclient0 runclient1 runclient2 clean

runserver:
	java com.ds18749.component.Server

runclient0:
	java com.ds18749.component.Client 0

runclient1:
	java com.ds18749.component.Client 1

runclient2:
	java com.ds18749.component.Client 2

runlfd:
	java com.ds18749.component.LocalFaultDetector

install-jdk:
	sudo apt install default-jdk

clean:
	find . -name *.class -delete
