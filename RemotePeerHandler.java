import java.net.*;
import java.io.*;

public class RemotePeerHandler implements Runnable 		// handshake 와 bitfield 메세지를 관리
{
	private Socket peerSocket = null;
	private InputStream in;
	private OutputStream out;
	private int connType;
	
	private HandshakeMessage handshakeMessage;
	
	String ownPeerId, remotePeerId;
	
	final int ACTIVECONN = 1;
	final int PASSIVECONN = 0;

	public void openClose(InputStream i, Socket socket)
	{
		try {
			i.close();
			i = socket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public RemotePeerHandler(Socket peerSocket, int connType, String ownPeerID) {
		
		this.peerSocket = peerSocket;
		this.connType = connType;
		this.ownPeerId = ownPeerID;
		try
		{
			in = peerSocket.getInputStream();       // input stream 은 왜 필요한거지? -> 처음 connection 시작될때만 ListeningThread 에서 소켓받고 이후 메세지 교환할때는 여기서 -> 바이트 스트림
			out = peerSocket.getOutputStream();		// ListeningThread 에서 스레드를 생성. 인풋 아웃풋 스트림으로 (저게 주고 받을때 메세지를 전달하는 역할을 하는거네요)
		} 
		catch (Exception ex) 
		{
			peerProcess.showLog(this.ownPeerId + " Error : " + ex.getMessage());
		}
	}
	
	public RemotePeerHandler(String add, int port, int connType, String ownPeerID) 
	{	
		try 
		{
			this.connType = connType;
			this.ownPeerId = ownPeerID;
			this.peerSocket = new Socket(add, port);			
		} 
		catch (UnknownHostException e) 
		{
			peerProcess.showLog(ownPeerID + " RemotePeerHandler : " + e.getMessage());
		} 
		catch (IOException e) 
		{
			peerProcess.showLog(ownPeerID + " RemotePeerHandler : " + e.getMessage());
		}
		this.connType = connType;
		
		try 
		{
			in = peerSocket.getInputStream();
			out = peerSocket.getOutputStream();
		} 
		catch (Exception ex) 
		{
			peerProcess.showLog(ownPeerID + " RemotePeerHandler : " + ex.getMessage());
		}
	}
	
	public boolean SendHandshake() 	// 핸드세이크 메세지를 보내고 ture 를 리턴함 즉 이 함수를 호출하면 헨드세이크 메세지를 보내는 동시에 투르를 리턴함
	{
		try 
		{
			out.write(HandshakeMessage.encodeMessage(new HandshakeMessage(MessageConstants.HANDSHAKE_HEADER, this.ownPeerId)));
		} 
		catch (IOException e) 
		{
			peerProcess.showLog(this.ownPeerId + " SendHandshake : " + e.getMessage());
			return false;
		}
		return true;
	}

	public boolean ReceiveHandshake() 
	{
		byte[] receivedHandshakeByte = new byte[32];
		try 
		{
			in.read(receivedHandshakeByte);
			handshakeMessage = HandshakeMessage.decodeMessage(receivedHandshakeByte);
			remotePeerId = handshakeMessage.getPeerIDString();
			
			//populate peerID to socket mapping
			peerProcess.peerIDToSocketMap.put(remotePeerId, this.peerSocket);
		} 
		catch (IOException e) 
		{
			peerProcess.showLog(this.ownPeerId + " ReceiveHandshake : " + e.getMessage());
			return false;
		}
		return true;
	}		

	public boolean SendRequest(int index)
	{
		try 
		{
			out.write(DataMessage.encodeMessage(new DataMessage( DataMessageType.DATA_MSG_REQUEST.getValue(), ConversionUtil.intToByteArray(index))));
		} 
		catch (IOException e) 
		{
			peerProcess.showLog(this.ownPeerId + " SendRequest : " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public boolean SendInterested()
	{
		try{
			out.write(DataMessage.encodeMessage(new DataMessage(DataMessageType.DATA_MSG_INTERESTED.getValue())));
		} 
		catch (IOException e){
			peerProcess.showLog(this.ownPeerId + " SendInterested : " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public boolean SendNotInterested()
	{
		try{
			out.write(DataMessage.encodeMessage(new DataMessage( DataMessageType.DATA_MSG_NOTINTERESTED.getValue())));
		} 
		catch (IOException e){
			peerProcess.showLog(this.ownPeerId + " SendNotInterested : " + e.getMessage());
			return false;
		}
		
		return true;
	}
	
	public boolean ReceiveUnchoke()
	{
		byte [] receiveUnchokeByte = null;
		
		try 
		{
			in.read(receiveUnchokeByte);
		} 
		catch (IOException e){
			peerProcess.showLog(this.ownPeerId + " ReceiveUnchoke : " + e.getMessage());
			return false;
		}
				
		DataMessage m = DataMessage.decodeMessage(receiveUnchokeByte);
		if(m.getMessageTypeString().equals(DataMessageType.DATA_MSG_UNCHOKE.getValue())){
			peerProcess.showLog(ownPeerId + "is unchoked by " + remotePeerId);
			return true;
		}
		else 
			return false;
	}
	
	public boolean ReceiveChoke()
	{
		byte [] receiveChokeByte = null;
	
		// Check whether the in stream has data to be read or not.
		try{
			if(in.available() == 0) return false;
		} 
		catch (IOException e){
			peerProcess.showLog(this.ownPeerId + " ReceiveChoke : " + e.getMessage());
			return false;
		}
		try{
			in.read(receiveChokeByte);
		} 
		catch (IOException e){
			peerProcess.showLog(this.ownPeerId + " ReceiveChoke : " + e.getMessage());
			return false;
		}
		DataMessage m = DataMessage.decodeMessage(receiveChokeByte);
		if(m.getMessageTypeString().equals(DataMessageType.DATA_MSG_CHOKE.getValue()))
		{
			// LOG 6:
			peerProcess.showLog(ownPeerId + " is CHOKED by " + remotePeerId);
			return true;
		}
		else 
			return false;
	}
	
	public boolean receivePeice()
	{
		byte [] receivePeice = null;
		
		try 
		{
			in.read(receivePeice);
		} 
		catch (IOException e) 
		{
			peerProcess.showLog(this.ownPeerId + " receivePeice : " + e.getMessage());
			return false;
		}
				
		DataMessage m = DataMessage.decodeMessage(receivePeice);
		if(m.getMessageTypeString().equals(DataMessageType.DATA_MSG_UNCHOKE.getValue()))
		{	
			// LOG 5:
			peerProcess.showLog(ownPeerId + " is UNCHOKED by " + remotePeerId);
			return true;
		}
		else 
			return false;

	}
	
	public void run() 
	{	
		byte []handshakeBuff = new byte[32];	//  32바이트 헤더 메세지 생성
		byte []dataBuffWithoutPayload = new byte[HandshakeType.DATA_MSG_LEN.getValue() + HandshakeType.DATA_MSG_TYPE.getValue()];	// actual 메세지의 payload 를 제외한 5바이트 배열을 만듬
		byte[] msgLength;
		byte[] msgType;
		DataMessageWrapper dataMsgWrapper = new DataMessageWrapper();

		try
		{
			if(this.connType == ACTIVECONN)		// activeconn 은 퍼스트 피어 외의 피어들
			{
				if(!SendHandshake())		// 핸드세이크 메세지를 보냈는지 체크 안보냈는지 체크 (함수를 호출하면서 핸드세이크 메세지도 보내게 됨)
				{
					peerProcess.showLog(ownPeerId + " HANDSHAKE sending failed.");
					System.exit(0);
				}
				else
				{
					peerProcess.showLog(ownPeerId + " HANDSHAKE has been sent...");
				}
				while(true)
				{
					in.read(handshakeBuff);	// 인풋스트림을 읽음
					handshakeMessage = HandshakeMessage.decodeMessage(handshakeBuff);	// 핸드세이크 메세지를 디코드
					if(handshakeMessage.getHeaderString().equals(MessageConstants.HANDSHAKE_HEADER))
					{
						
						remotePeerId = handshakeMessage.getPeerIDString();		// 핸드세이크가 온 피어아이디를 스트링으로 저장
						
						peerProcess.showLog(ownPeerId + " makes a connection to Peer " + remotePeerId);
						
						peerProcess.showLog(ownPeerId + " Received a HANDSHAKE message from Peer " + remotePeerId);
						
						//populate peerID to socket mapping
						peerProcess.peerIDToSocketMap.put(remotePeerId, this.peerSocket);
						break;
					}
					else
					{
						continue;
					}		
				}
				
				// Sending BitField...
				DataMessage d = new DataMessage(DataMessageType.DATA_MSG_BITFIELD.getValue(), peerProcess.ownBitField.encode());
				byte  []b = DataMessage.encodeMessage(d);
				out.write(b);			// bitfield 메세지 초기에 보내는 스트림
				peerProcess.peerCommonMapUtil.getRemotePeerInfo(remotePeerId).state = 8;		// 핸드 세이크 메세지를 보내고 이후 비트필드 메세지 보내고 스테이트 8로 감
			}
			//Passive connection
			else	// 퍼스트 피어인 경우
			{
				while(true)
				{
					in.read(handshakeBuff);
					handshakeMessage = HandshakeMessage.decodeMessage(handshakeBuff);		// 읽어드린 핸드세이크 메세지를 디코드
					if(handshakeMessage.getHeaderString().equals(MessageConstants.HANDSHAKE_HEADER))
					{
						remotePeerId = handshakeMessage.getPeerIDString();		// 피어 아이디 스트링 타입으로 추출
						
						peerProcess.showLog(ownPeerId + " makes a connection to Peer " + remotePeerId);
						peerProcess.showLog(ownPeerId + " Received a HANDSHAKE message from Peer " + remotePeerId);
						
						//populate peerID to socket mapping
						peerProcess.peerIDToSocketMap.put(remotePeerId, this.peerSocket);
						break;
					}
					else
					{
						continue;
					}		
				}
				if(!SendHandshake())		// 핸드세이크 메세지 보내는데 실패하는 경우
				{
					peerProcess.showLog(ownPeerId + " HANDSHAKE message sending failed.");
					System.exit(0);
				}
				else		// 핸드세이크 메세지 보낸 경우
				{
					peerProcess.showLog(ownPeerId + " HANDSHAKE message has been sent successfully.");
				}
				
				peerProcess.peerCommonMapUtil.getRemotePeerInfo(remotePeerId).state = 2;	// 서버는 핸드세이크 메세지 보내고 스테이트 2로 진입. 비트필드는 안보냄 어차피 파일 가지고 있는걸 아니까
			}
			// receive data messages continuously 
			while(true)		// 퍼스트 피어와 다른 피어 모두 수행하는 동작
			{
				
				int headerBytes = in.read(dataBuffWithoutPayload);	// actual 메세지의 payload 를 제외한 바이트 배열을 정수형으로 바꿈
				
				if(headerBytes == -1)
					break;

				msgLength = new byte[HandshakeType.DATA_MSG_LEN.getValue()];		// 4바이트의 length 헤더를 만듬
				msgType = new byte[HandshakeType.DATA_MSG_TYPE.getValue()];		// 1 바이트의 type 헤더를 만듬
				System.arraycopy(dataBuffWithoutPayload, 0, msgLength, 0, HandshakeType.DATA_MSG_LEN.getValue());		// 받은 핸드세이크 메세지를 복사하는 과정
				System.arraycopy(dataBuffWithoutPayload, HandshakeType.DATA_MSG_LEN.getValue(), msgType, 0, HandshakeType.DATA_MSG_TYPE.getValue());
				DataMessage dataMessage = new DataMessage();
				dataMessage.setMessageLength(msgLength);
				dataMessage.setMessageType(msgType);
				if(dataMessage.getMessageTypeString().equals(DataMessageType.DATA_MSG_CHOKE.getValue())			// 메세지 타입이 어떤지 확인
						||dataMessage.getMessageTypeString().equals(DataMessageType.DATA_MSG_UNCHOKE.getValue())
						||dataMessage.getMessageTypeString().equals(DataMessageType.DATA_MSG_INTERESTED.getValue())
						||dataMessage.getMessageTypeString().equals(DataMessageType.DATA_MSG_NOTINTERESTED.getValue())){
					dataMsgWrapper.setDataMsg(dataMessage);
					dataMsgWrapper.setFromPeerID(this.remotePeerId);
					peerProcess.addToMsgQueue(dataMsgWrapper);		// 받은 메세지를 MsgQueue 에 저장
				}
				else {	//have bitfield request piece 메세지인 경우
					int bytesAlreadyRead = 0;
					int bytesRead;
					byte []dataBuffPayload = new byte[dataMessage.getMessageLengthInt()-1];		// 페이로드 부분을 저장하기 위한 버퍼 생성
					while(bytesAlreadyRead < dataMessage.getMessageLengthInt()-1){		// 메세지의 페이로드 부분을 읽어옴
						bytesRead = in.read(dataBuffPayload, bytesAlreadyRead, dataMessage.getMessageLengthInt()-1-bytesAlreadyRead);
						if(bytesRead == -1)
							return;
						bytesAlreadyRead += bytesRead;
					}
					
					byte []dataBuffWithPayload = new byte [dataMessage.getMessageLengthInt()+HandshakeType.DATA_MSG_LEN.getValue()];
					System.arraycopy(dataBuffWithoutPayload, 0, dataBuffWithPayload, 0, HandshakeType.DATA_MSG_LEN.getValue() + HandshakeType.DATA_MSG_TYPE.getValue());
					System.arraycopy(dataBuffPayload, 0, dataBuffWithPayload, HandshakeType.DATA_MSG_LEN.getValue() + HandshakeType.DATA_MSG_TYPE.getValue(), dataBuffPayload.length);
					// 메세지 페이로드 부분을 카피해 와서 처리함.
					DataMessage dataMsgWithPayload = DataMessage.decodeMessage(dataBuffWithPayload);
					dataMsgWrapper.setDataMsg(dataMsgWithPayload);
					dataMsgWrapper.setFromPeerID(remotePeerId);
					peerProcess.addToMsgQueue(dataMsgWrapper);		// 최종적으로 페이로드를 msgQueue 에 저장
					dataBuffPayload = null;
					dataBuffWithPayload = null;
					bytesAlreadyRead = 0;
					bytesRead = 0;
				}
			}
		}
		catch(IOException e){
			peerProcess.showLog(ownPeerId + " run exception: " + e);
		}	
		
	}
	
	public void releaseSocket() {		// 통신 종료
		try {
			if (this.connType == PASSIVECONN && this.peerSocket != null) {
				this.peerSocket.close();
			}
			if (in != null) {
				in.close();
			}
			if (out != null)
				out.close();
		} catch (IOException e) {
			peerProcess.showLog(ownPeerId + " Release socket IO exception: " + e);
		}
	}
}