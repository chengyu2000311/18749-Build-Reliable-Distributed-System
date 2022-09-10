JC=javac

targets:
	$(JC) com/ds18749/component/*.java

.PHONY: runserver runclient0 runclient1 runclient2 clean

runserver:
	java com.ds18749.component.Server

runclient1:
	java com.ds18749.component.Client 1

runclient2:
	java com.ds18749.component.Client 2

runclient3:
	java com.ds18749.component.Client 3

runlfd:
	java com.ds18749.component.LocalFaultDetector

install-jdk:
	sudo apt install default-jdk

clean:
	find . -name *.class -delete
