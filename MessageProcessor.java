import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;

public class MessageProcessor implements Runnable {		// Runnable 은 run() 이라는 메소드만 가지고 있음. 따라서 Runnalbe 을 implements 한 클래스에서 run 메소드를 오버라이딩하여 스레드가 동작하면 어떤 작업을 하는지 명시해줘야함.
	private static boolean running = true;
	public static int peerState = -1;
	RandomAccessFile raf;
	
	// constructor
	public MessageProcessor(String PeerID_pthis){}
	
	// constructor
	public MessageProcessor(){}		// 디폴트 컨스트럭터
	
	public void pTS(String dataType, int state)	{
		peerProcess.showLog("Message Processor : msgType = "+ dataType + " State = "+state);
	}

	@Override
	public void run() {				// run 메소드를 오버라이딩
		DataMessage d;
		DataMessageWrapper dataWrapper;
		String msgType;
		String rPeerId;
				
		while(running) {
			dataWrapper  = peerProcess.removeFromMsgQueue();		// MsgQueue(받은 메세지는 queue 에 저장) 에서 꺼냄 (두번째 peer가 생성되어야 채워짐)
			while(dataWrapper == null)			// datawrapper 가 없으면 Thread 를 sleep 시킴
			{
				Thread.currentThread();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				   e.printStackTrace();
				}
				dataWrapper  = peerProcess.removeFromMsgQueue();
			}
			
			d = dataWrapper.getDataMsg();		// MsgQueue 에서 꺼낸 메세지를 변수로 받음
			
			msgType = d.getMessageTypeString();		// 메세지의 타입을 문자열로 저장
			rPeerId = dataWrapper.getFromPeerID();	// 어느 피어로부터 온 메세지인지 피어아이디 추출
			int state = peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state;		// RemotePeerInfo 클래스의 변수, 해시테이블에서 받아옴 (state 숫자 정의가 명확하지 않은데 여기에 따라 행동결정)
			if(msgType.equals(DataMessageType.DATA_MSG_HAVE.getValue()) && state != 14)		// 받은 메세지가 'have' message 일 경우, state 14는 해당 피어로부터 초크 메세지를 받았다는 것 즉 해당 피어에게 초크 당했음을 의미
			{
				peerProcess.showLog(peerProcess.peerID + " receieved HAVE message from Peer " + rPeerId);  //'have' 메세지에 대한 로그
				if(isInterested(d, rPeerId))		// 메세지를 보낸 피어(rPeerID) 가 내가 없는 피스를 가지고 있으면 실행되는 케이스, isInterested 를 실행하면서 rPeerID 가 보낸 비트필드 정보가 해시테이블에 저장
				{
					//peerProcess.showLog(peerProcess.peerID + " is interested in Peer " + rPeerId);
					sendInterested(peerProcess.peerIDToSocketMap.get(rPeerId), rPeerId);		// rPeerID 로 인터레스트 메세지를 보냄
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 9;		// 인터레스트 메세지를 보내고 state 9 로 해시 테이블 갱신, 해당 피어로 interest 메세지를 보냈음을 의미
				}	
				else
				{
					//peerProcess.showLog(peerProcess.peerID + "is not interested " + rPeerId);
					sendNotInterested(peerProcess.peerIDToSocketMap.get(rPeerId), rPeerId);		// 관심있는 피스가 없다면 not interested 메세지를 보냄
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 13;		// 해당피어로 state 13 을 해시테이블에 갱신. not interested 메세지를 보냇음을 의미
				}
			}
			else
			{
			 switch (state)		// state 에 들어가있는 숫자와 일치하는 숫자의 케이스가 실행
			 {
			 
			 case 2:		// 퍼스트 피어가 핸드세이크 메세지를 보내고 진입하는 스테이트
			   if (msgType.equals(DataMessageType.DATA_MSG_BITFIELD.getValue())) {		// 꺼낸메세지 타입이 비트필드 메세지가 맞는지 확인
		 		  peerProcess.showLog(peerProcess.peerID + " receieved a BITFIELD message from Peer " + rPeerId);		// 맞으면 비트필드 받았음을 로그로 기록
	 			  sendBitField(peerProcess.peerIDToSocketMap.get(rPeerId), rPeerId);		// 비트 필드 메세지를 보냄
 				  peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 3;  		// 비트 필드 메세지를 보내고 state 3으로 기록
			   }
			   break;
			 
			 case 3:		// 내가 have 메세지 또는 비트 필드 메세지를 보내고 답을 기다리고 있는 상태
			   if (msgType.equals(DataMessageType.DATA_MSG_NOTINTERESTED.getValue())) {		// 메세지 타입이 not interested 인지 확인
					peerProcess.showLog(peerProcess.peerID + " receieved a NOT INTERESTED message from Peer " + rPeerId);
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).isInterested = 0;		// 해시테이블에 isInterest 값을 0으로 변경 즉 not interest 를 받았음을 의미
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 5;		// 해당 피어 아이디에 해당하는 해시테이블의 state 값을 5로 변경
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).isHandShaked = 1;		// handshake 메세지를 받았음을 의미
			   }
			   else if (msgType.equals(DataMessageType.DATA_MSG_INTERESTED.getValue())) {		// 메세지 타입이 interested 인지 확인
					peerProcess.showLog(peerProcess.peerID + " receieved an INTERESTED message from Peer " + rPeerId);
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).isInterested = 1;		// 해시테이블에 isInterested 를 1로 반경
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).isHandShaked = 1;		// handshake 메세지를 받았음을 의미
					
					if(!peerProcess.peerCommonMapUtil.preferedNeighborsContainsKey(rPeerId) && !peerProcess.peerCommonMapUtil.unchokedNeighborsContainsKey(rPeerId))
					{		// preferedNeighbor, unchokedNeighbor 둘 다 아닌 경우
						sendChoke(peerProcess.peerIDToSocketMap.get(rPeerId), rPeerId);		// 초크메세지를 보냄
						peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).isChoked = 1;		// 초크되었음을 업데이트함
						peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state  = 6;		// 해당 피어의 state 를 6으로 설정
					}
					else
					{
						peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).isChoked = 0;		// 위의 경우가 아니면 프리퍼네이버나 언초크 둘 중 하나
						sendUnChoke(peerProcess.peerIDToSocketMap.get(rPeerId), rPeerId);		// 언초크 메세지를 보냄
						peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 4 ;		// 해당 피어의 state 를 4로 업데이트
					}
			   }
			   break;
			   
			 case 4:		// 해당 피어에게 언초크 메세지를 보낸 상태를 의미
				 if (msgType.equals(DataMessageType.DATA_MSG_REQUEST.getValue())) {		// request 메세지를 받은 경우
					//peerProcess.showLog(peerProcess.peerID + " receieved a REQUEST message from Peer " + rPeerId);
						sendPeice(peerProcess.peerIDToSocketMap.get(rPeerId), d, rPeerId);		// 해당 피어에게 piece 를 보냄
						// Decide to send CHOKE or UNCHOKE message
						if(!peerProcess.peerCommonMapUtil.preferedNeighborsContainsKey(rPeerId) && !peerProcess.peerCommonMapUtil.unchokedNeighborsContainsKey(rPeerId))
						{		// 프리퍼 네이버, 언초크네이버가 아닌경우는 마찬가지로 초크 메세지를 보냄
							sendChoke(peerProcess.peerIDToSocketMap.get(rPeerId), rPeerId);		// 초크메세지를 보냄
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).isChoked = 1;
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 6;
						}  
				 }
				 break;
				 
			 case 8:		// 퍼스트 피어 외의 피어가 초기에 핸드세이크와 비트필드를 보내고 난 이후 진입하는 스테이트
				 if (msgType.equals(DataMessageType.DATA_MSG_BITFIELD.getValue())) {		// 스테이트 8에서 비트메세지를 받은 경우
						//Decide if interested or not.
						if(isInterested(d,rPeerId))
						{
							//peerProcess.showLog(peerProcess.peerID + " is interested in Peer " + rPeerId);
							sendInterested(peerProcess.peerIDToSocketMap.get(rPeerId), rPeerId);
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 9;		// 스테이트 9는 해당 피어에게 인터레스트 메세지를 보냈음을 의미
						}	
						else
						{
							//peerProcess.showLog(peerProcess.peerID + " is not interested in Peer " + rPeerId);
							sendNotInterested(peerProcess.peerIDToSocketMap.get(rPeerId), rPeerId);
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 13;		// 스테이트 13은 해당 피어로부터 받을게 없음을 의미
						}
				 }
				 break;
				 
			 case 9:		// 해당 피어에게 인터레스트 메세지를 보낸 상태
				 if (msgType.equals(DataMessageType.DATA_MSG_CHOKE.getValue())) {		// 초크메세지를 받은 경우
						peerProcess.showLog(peerProcess.peerID + " is CHOKED by Peer " + rPeerId);
						peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 14;		// 스테이트 14로 변경
				 }
				 else if (msgType.equals(DataMessageType.DATA_MSG_UNCHOKE.getValue())) {		// 언초크 메세지를 받은경우
						peerProcess.showLog(peerProcess.peerID + " is UNCHOKED by Peer " + rPeerId);
						int firstdiff = peerProcess.ownBitField.returnFirstDiff(peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).bitField);		// 비트 필드를 비교해서 내가 가지고 있지 않은 첫번째 비트를 리턴
						if(firstdiff != -1)		// -1은 상대방이 가지고 있고 내가 가지고 있지 않은 경우가 없는 경우
						{
							//peerProcess.showLog(peerProcess.peerID + " is Requesting PIECE " + firstdiff + " from peer " + rPeerId);
							sendRequest(peerProcess.peerIDToSocketMap.get(rPeerId), firstdiff, rPeerId);		// firstdiff 에 해당하는 비트부터 보내달라고 리퀘스트
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 11;		// 리퀘스트 메세지를 보내고 스테이트 11로 바꿈 - request 보낸 상태
							// Get the time when the request is being sent.
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).startTime = new Date();		// 리퀘스트 후 타이머 돌림
						}
						else
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 13;		// 즉 상대방에게 내가 받을 파일이 없는 경우는 해당 피어의 스테이트를 13으로 업데이트
				 }
				 break;
				 
			 case 11:		// 해당 피어에게 리퀘스트를 보내고 응답을 기다리는 상태. 즉 답은 piece 메세지로 올 것
				 if (msgType.equals(DataMessageType.DATA_MSG_PIECE.getValue())) {	// piece 메세지를 받은 경우
					    byte[] buffer = d.getPayload();						// 받은 메세지의 payload 를 버퍼에 저장
						peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).finishTime = new Date();		// 리퀘스트 메세지를 보내고 스타트 한 시간을 종료 시킴
						long timeLapse = peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).finishTime.getTime() - 	// 걸린 시간 산출
									peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).startTime.getTime() ;
						
						peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).dataRate = ((double)(buffer.length + HandshakeType.DATA_MSG_LEN.getValue() + HandshakeType.DATA_MSG_TYPE.getValue())/(double)timeLapse) * 100;
						// 다운로드 속도 산출
						Piece p = Piece.decodePiece(buffer);	// 받은 payload 를 p 로 복사
						peerProcess.ownBitField.updateBitField(rPeerId, p);			// 해당 피어로부터 받은 비트필드 메세지에 받은 피스 정보를 업데이트
						
						int toGetPeiceIndex = peerProcess.ownBitField.returnFirstDiff(peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).bitField);	// 다시 내게 없는 피스 인덱스를 산출함
						if(toGetPeiceIndex != -1)	// 아직 더 받을 게 있는 경우
						{
							//peerProcess.showLog(peerProcess.peerID + " Requesting piece " + toGetPeiceIndex + " from peer " + rPeerId);
							sendRequest(peerProcess.peerIDToSocketMap.get(rPeerId),toGetPeiceIndex, rPeerId);	// 다시 리퀘스트를 보냄
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state  = 11;	// 리퀘스트 응답을 기다리는 스테이트 11로 설정
							// Get the time when the request is being sent.
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).startTime = new Date();	// 다시 타이머 돌림
						}
						else
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 13;	// 리퀘스트 할게 없으면 13으로 스테이트 바꿈 (스테이트 13은 해당 피어로부터는 받을 것이 없음을 의미)
						
						//updates remote peerInfo
						peerProcess.readPeerInfoAgain();	// Peerconfig.cfg 를 다시 읽어들임
						
						Enumeration<String> keys = peerProcess.peerCommonMapUtil.getRemotePeerInfoHashKeys();
						while(keys.hasMoreElements())
						{
							String key = (String)keys.nextElement();
							RemotePeerInfo pref = peerProcess.peerCommonMapUtil.getRemotePeerInfo(key);
							
							if(key.equals(peerProcess.peerID))continue;		// 이건 peerProcess 가 돌아가는 피시의 피어아이디와 동일한 key 값이 나온경우 -> 고려 안하고 컨티뉴
							//peerProcess.showLog(peerProcess.peerID + ":::: isCompleted =" + pref.isCompleted + " isInterested =" + pref.isInterested + " isChoked =" + pref.isChoked);
							if (pref.isCompleted == 0 && pref.isChoked == 0 && pref.isHandShaked == 1)
							{		// 아직 완료되지 않은 피어에게 메세지를 보냄
								//peerProcess.showLog(peerProcess.peerID + " isCompleted =" + pref.isCompleted + " isInterested =" + pref.isInterested + " isChoked =" + pref.isChoked);
								sendHave(peerProcess.peerIDToSocketMap.get(key), key);	// 내가 받은 피스가 있으니까 원래 내게 피스가 없었다고 하더라고 받았으니까 새로 생김. 따라서 have 메세지를 보냄 (즉 have 메세지는 piece 를 새로 받은 경우에만 보냄)
								peerProcess.peerCommonMapUtil.getRemotePeerInfo(key).state = 3;		// have 메세지를 보내고 응답을 기다리는 상태
								
							} 
							
						}
										
						buffer = null;
						d = null;		// 버퍼를 초기화 해줌
			
				 }
				 else if (msgType.equals(DataMessageType.DATA_MSG_CHOKE.getValue())) {	// 리퀘스트 메세지의 응답으로 초크메세지를 받은 경우
						peerProcess.showLog(peerProcess.peerID + " is CHOKED by Peer " + rPeerId);
						peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 14;		// 초크 메세지를 받았음을 의미하는 스테이트 14로 업데이트
				 }
				 break;
				 
			 case 14:		// 해당 피어에게 초크 메세지를 받은 상태
				 if (msgType.equals(DataMessageType.DATA_MSG_HAVE.getValue())) {	// 초크 메세지를 받은 피어에게서 해브메세지를 받은경우
						//Decide if interested or not.
						if(isInterested(d,rPeerId))
						{
							//peerProcess.showLog(peerProcess.peerID + " is interested in Peer " + rPeerId);
							sendInterested(peerProcess.peerIDToSocketMap.get(rPeerId), rPeerId);
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 9;
						}	
						else
						{
							//peerProcess.showLog(peerProcess.peerID + " is not interested in Peer " + rPeerId);
							sendNotInterested(peerProcess.peerIDToSocketMap.get(rPeerId), rPeerId);
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 13;
						}
				 }
				 else if (msgType.equals(DataMessageType.DATA_MSG_UNCHOKE.getValue())) {	// 언초크 메세지를 받은경우
						peerProcess.showLog(peerProcess.peerID + " is UNCHOKED by Peer " + rPeerId);
						peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).state = 14;	// 언초크메세지를 받았음을 의미하는 스테이트 14로 업데이트(이거 이상한데? 초크 받은 상태에서 언초크 메세지를 받아도 다시 초크 받았다고 기록하는거아님?)
				 }
				 break;
				 
			 }
			}

		}
	}
	 
	private void sendRequest(Socket socket, int pieceNo, String remotePeerID) {		// request 메세지를 바이트 어레이로 인코딩

		// Byte2int....
		byte[] pieceByte = new byte[HandshakeType.PIECE_INDEX_LEN.getValue()];
		for (int i = 0; i < HandshakeType.PIECE_INDEX_LEN.getValue(); i++) {
			pieceByte[i] = 0;
		}

		byte[] pieceIndexByte = ConversionUtil.intToByteArray(pieceNo);
		System.arraycopy(pieceIndexByte, 0, pieceByte, 0,
						pieceIndexByte.length);
		DataMessage d = new DataMessage(DataMessageType.DATA_MSG_REQUEST.getValue(), pieceByte);
		byte[] b = DataMessage.encodeMessage(d);
		SendData(socket, b);

		pieceByte = null;
		pieceIndexByte = null;
		b = null;
		d = null;
	}

	private void sendPeice(Socket socket, DataMessage d, String remotePeerID) { //d == requested message 즉 피스를 보내줌
	
		CommonProperties CommonProperties = new CommonProperties();
		byte[] bytePieceIndex = d.getPayload();
		int pieceIndex = ConversionUtil.byteArrayToInt(bytePieceIndex);
		
		peerProcess.showLog(peerProcess.peerID + " sending a PIECE message for piece " + pieceIndex + " to Peer " + remotePeerID);
		
		byte[] byteRead = new byte[CommonProperties.getPieceSize()];
		int noBytesRead = 0;
		
		File file = new File(peerProcess.peerID,CommonProperties.getFileName());
		try	{
			raf = new RandomAccessFile(file,"r");
			raf.seek(pieceIndex*CommonProperties.getPieceSize());
			noBytesRead = raf.read(byteRead, 0, CommonProperties.getPieceSize());		// 파일정보를 읽어옴
		} catch (IOException e) {
			peerProcess.showLog(peerProcess.peerID + " ERROR in reading the file : " +  e.toString());
		}
		
		if( noBytesRead == 0) {
			peerProcess.showLog(peerProcess.peerID + " ERROR :  Zero bytes read from the file !");
		} else if (noBytesRead < 0) {
			peerProcess.showLog(peerProcess.peerID + " ERROR : File could not be read properly.");
		}
		
		byte[] buffer = new byte[noBytesRead + HandshakeType.PIECE_INDEX_LEN.getValue()];		// 파일 바이트 정보를 버퍼에 저장
		System.arraycopy(bytePieceIndex, 0, buffer, 0, HandshakeType.PIECE_INDEX_LEN.getValue());
		System.arraycopy(byteRead, 0, buffer, HandshakeType.PIECE_INDEX_LEN.getValue(), noBytesRead);

		DataMessage sendMessage = new DataMessage(DataMessageType.DATA_MSG_PIECE.getValue(), buffer);
		byte[] b =  DataMessage.encodeMessage(sendMessage);		// 메세지를 보내기 위해 바이트 어레이로 변경
		SendData(socket, b);
		
		//release memory
		buffer = null;
		byteRead = null;
		b = null;
		bytePieceIndex = null;
		sendMessage = null;
		
		try{
			raf.close();
		}
		catch(Exception e){}
	}
	
	private void sendNotInterested(Socket socket, String remotePeerID) {
		peerProcess.showLog(peerProcess.peerID + " sending a NOT INTERESTED message to Peer " + remotePeerID);
		DataMessage d =  new DataMessage(DataMessageType.DATA_MSG_NOTINTERESTED.getValue());
		byte[] msgByte = DataMessage.encodeMessage(d);
		SendData(socket,msgByte);
	}

	private void sendInterested(Socket socket, String remotePeerID) {		//rPeerID 에 해당하는 피어로 소켓을 보냄
		peerProcess.showLog(peerProcess.peerID + " sending an INTERESTED message to Peer " + remotePeerID);
		DataMessage d =  new DataMessage(DataMessageType.DATA_MSG_INTERESTED.getValue());		// interest 메세지 생성
		byte[] msgByte = DataMessage.encodeMessage(d);		// 메세지를 byte 배열로 인코딩
		SendData(socket,msgByte);		// 바이트 배열로 인코딩된 메세지를 보내고자 하는 피어 아이디에 해당하는 소켓으로 전송
	}
	
//  Compare the bitfield and send TRUE if there is any extra data	
	private boolean isInterested(DataMessage d, String rPeerId) {		
		
		BitField b = BitField.decode(d.getPayload());		// Bitfield 메세지는 페이로드에 있으므로 디코딩해서 비트필드 b에 저장
		peerProcess.peerCommonMapUtil.getRemotePeerInfo(rPeerId).bitField = b;		// 메세지를 보낸 피어의 비트필드 정보를 해시테이블에 저장 (이걸로 해당 피어가 필요한 피스를 가지고 있는 확인 가능)

		if(peerProcess.ownBitField.compare(b))return true;		// 상대방이 내가 없는 피스를 가지고 있으면 true 를 리턴
		return false;
	}

	private void sendUnChoke(Socket socket, String remotePeerID) {		// 언초크 메세지를 보냄

		peerProcess.showLog(peerProcess.peerID + " sending UNCHOKE message to Peer " + remotePeerID);
		DataMessage d = new DataMessage(DataMessageType.DATA_MSG_UNCHOKE.getValue());
		byte[] msgByte = DataMessage.encodeMessage(d);
		SendData(socket,msgByte);
	}

	private void sendChoke(Socket socket, String remotePeerID) {		// 초크 메세지를 보냄
		peerProcess.showLog(peerProcess.peerID + " sending CHOKE message to Peer " + remotePeerID);
		DataMessage d = new DataMessage(DataMessageType.DATA_MSG_CHOKE.getValue());
		byte[] msgByte = DataMessage.encodeMessage(d);
		SendData(socket,msgByte);
	}

	private void sendBitField(Socket socket, String remotePeerID) {
		peerProcess.showLog(peerProcess.peerID + " sending BITFIELD message to Peer " + remotePeerID);
		byte[] encodedBitField = peerProcess.ownBitField.encode();		// 내가 가지고 있는 비트 필드의 페이로드를 바이트로 변환

		DataMessage d = new DataMessage(DataMessageType.DATA_MSG_BITFIELD.getValue(), encodedBitField);		// wrapper 에 실음
		SendData(socket,DataMessage.encodeMessage(d));		// 메세지를 보냄
		
		encodedBitField = null;		// 초기화 시킴
	}
	
	private void sendHave(Socket socket, String remotePeerID) {
		peerProcess.showLog(peerProcess.peerID + " sending HAVE message to Peer " + remotePeerID);
		byte[] encodedBitField = peerProcess.ownBitField.encode();
		DataMessage d = new DataMessage(DataMessageType.DATA_MSG_HAVE.getValue(), encodedBitField);
		SendData(socket,DataMessage.encodeMessage(d));
		
		encodedBitField = null;
	}
	
	private int SendData(Socket socket, byte[] encodedBitField) {
		OutputStream out = null;
		try {
			out = socket.getOutputStream();
			out.write(encodedBitField);
		} catch (IOException e) {
            e.printStackTrace();
			return 0;
		}
		return 1;
	}
}
