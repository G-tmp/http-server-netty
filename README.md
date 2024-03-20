# http-server-netty



### critical drawback

- Not suitable for uploading large files. ```HttpPostRequestDecoder```receive whole http content then parse it, cause temporary file occupies large mount of storage on ```/tmp```, and memory leak.

- manually read and parse HttpContent is a solution probably.

- [OutOfMemory error while doing a file upload of size 7GB through Netty](https://github.com/netty/netty/issues/3559)

