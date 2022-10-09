# 18-749 Build Reliable Distributed System
## Milestone 0
- Implement simple client-server application
- 3 clients and 1 server
- open it in IntelliJ and run

## Milestone 1
- Modified the client-server from mult-thread to multi-process.
- Added Makefile under src/.
- To compile:
    $ make 
- Start the server:
    $ make runserver
- Start the client 0:
    $ make runclient 0.

## Milestone 2
- Add Global Fault Detector.
- Let Client discard duplicate responses from servers.
- modify Makefile under src/.
- To compile:
  $ make
- Start the server with id:
  $ make runserver id=1
- Start the client with id:
  $ make runclient id=1.
- Start the global fault detector:
  $ make rungfd
- Start the local fault detector with id:
  $ make runlfd id=1