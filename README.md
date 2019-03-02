This is a Java communications library that mimics the standard Java Socket API, but supports a pluggable-framework that allows the underlying medium of communication to vary tremendously.

Rather than using sockets as the transport layer for socket communication, the framework uses whatever the pluggable implementation has implemented, which can vary wildly. For instance, currently supported is the ability to map all socket communication to a local/networked filesystem, or through an FTP server, or through an IRC channel, or through Amazon S3.

The participants of this communication do not communicate directly; they actually communicate through the transport layer's mapping to this other medium of communication.

In one pluggable implementation, rather than transmitting the data through TCP-IP sockets, the data is written to the file system. The API is mapped in a way that exactly reflects the behaviour and semantics of TCP communication. In this scenario, the TCP stack is not utilized all all. All server listens, socket connections, name resolution, and data transmission is performed on a filesystem shared between the two or more programs/systems/entities involved in the communications.

In another (FTP/SFTP), all participants looking to communicate with one another connect to a common FTP (or SFTP) server. The pluggable implementation listens for files posted to the FTP server, and translates these files into server listens, socket connections, and data transmissions. In this way, the communicators are never actually connected to one another directly. The FTP server is the intermediary for all communication.

The project also includes a simple proxy redirection server that is used to add support for non-Java programs, or programs that are not implemented using SocketAnywhere?. This allows SocketAnywhere?-based Java programs to access these programs through the frameworks medium of message exchange, without underlying support from the target program.

This framework also has the advantage of allowing two or more hosts to entirely ignore network topology when communicating to one another, assuming that the participating nodes are able to connect to the existing intermediary (ftp server/amazon s3/irc/shared network file system/etc).

Pluggable communication implementations currently include:
* Local/networked file system
* FTP/SFTP
* IRC (Internet Relay Chat)
* Amazon S3
* Apache Virtual Filesystem (VFS) 

