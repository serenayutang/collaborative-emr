#6650 final project

We have also record a demo video. As we need to demonstrate all function in details, 
the video is a litte bit long(about 12 minute). Please feel to speed up when watching.
 
#Getting start

before getting start, please add commons-io-2.11.0.jar, log4j-1.2.17.jar, rmiio-2.1.2.jar
to the library.

Start central server:

Firstly, start central server and input six ports number for central server and affiliated 
servers. Like "19000 19001 19002 19003 19004 19005", 19000 for central server, others for
affiliated server.


Start Client:
Secondly, run client. Connect to central server, then input commands.

Start Admin:
Run admin if need to kill or restart a server.


# Testing

All related files will be save in dir like ./server_data_19001,./server_data_19002..


#Client Command:

- help: show help message 

- register Name PWD: register a doctor account with name and password

example:
	-register tom 123456
	
- login Name PWD: login using name and password, please login first before create or edit any 
file.

example:
	-login tom 123456

- create Documentname sectionNum: create a new document dir with give named, in the dir it will
create sectionNum files, index from 0. 

example:
	-create a 3


- edit DOC SEC: choose a document to edit

    - edit a 0
    - edit a 1

- endedit: to stop editing current section
    
- logout: to logout

- list: to list all the documents

- share UserName DOC: to share a document with another user

  - share Tom a

- unread: to get all unread messages


- send TEXT: send the TEXT message to user who is editing the same document
  
  - send hello
  
 
 #Admin:
 
- kill SERVER: shut down a server.
	-kill 19001
- restart SERVER: restart a server.
	-restart 19001


