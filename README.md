# Computer-Networks
## Project Overview
This is a P2P file sharing software similar to BitTorrent. Among BitTorrent's interesting features, I implemented the choking-unchoking mechanism which is one of the most important features of BitTorrent.

## How to use
* Set two the two configuration files: `Common.cfg` and `PeerInfo.cfg`.
* Run `peerProcess.java` in all the sharing PCs. For example, if peer ID is 1001, the comman will be `java peerProcess 1001`.

## Configuration Files
There are two configuration files which the peerProcess should read. The common properties used by all peers are specified in the file `Common.cfg` as follows:

| Property              | Value      |
| :---------------------------: | :-----------: |
| NumberOfPreferredNeighbors  | 2           |
| UnchokingInterval           | 5           |
| OptimisticUnchokingInterval | 15          |
| FileName                    | TheFile.dat |
| FileSize                    | 10000232    |
| PieceSize                   | 32768       |

The unit of UnchokingInterval and OptimisticUnchokingInterval is in seconds. The FileName property specifies the name of a file in which all peers are interested. FileSize specifies the size of the file in bytes. PieceSize specifies the size of a piece in bytes. In the above example, the file size is 10,000,232 bytes and the piece size is 32,768 bytes. Then the number of pieces of this file is 306. Note that the size of the last piece is only 5,992 bytes. Note that the file `Common.cfg` serves like the metainfo file in BitTorrent. Whenever a peer starts, it should read the file `Common.cfg` and set up the corresponding variables.

The peer information is specified in the file PeerInfo.cfg in the following format:

`[Peer ID] [IP address] [listening port] [has file or not]`

The following is an example of file `PeerInfo.cfg`.

`1001 10.136.27.164 10000 1`

`1002 10.136.136.33 10000 0`

Each line in file PeerInfo.cfg represents a peer. The first column is the peer ID, which is a positive integer number. The second column is the IP adress where the peer is. The third column is the port number at which the peer listens. The port numbers of the peers may be different from each other. The fourth column specifies whether it has the file or not. We only have two options here. ‘1’ means that the peer has the complete file and ‘0’ means that the peer does not have the file. We do not consider the case where a peer has only some pieces of the file. Note, however, that more than one peer may have the file. Note also that the file `PeerInfo.cfg` serves like a tracker in BitTorrent.
