JC=javac

targets:
	$(JC) com/ds18749/component/*.java

.PHONY: runserver runclient0 runclient1 runclient2 clean

rungfd:
	java com.ds18749.component.GlobalFaultDetector

runserver:
	java com.ds18749.component.Server $(id)


runclient:
	java com.ds18749.component.Client $(id)

runlfd:
	java com.ds18749.component.LocalFaultDetector $(id)

install-jdk:
	sudo apt install default-jdk

clean:
	find . -name *.class -delete
