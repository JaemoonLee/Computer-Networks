import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class peerProcess {
	
	public ServerSocket listeningSocket = null;
	public int LISTENING_PORT;
	public String PEER_IP = null;
	public static String peerID;
	public int myPeerIndex;
	public Thread listeningThread; 
	public static boolean isFinished = false;
	public static BitField ownBitField = null;
	public static volatile Timer timerPref;
	public static volatile Timer timerUnChok;
	public static volatile PeerCommonMapUtil peerCommonMapUtil = new PeerCommonMapUtil();
	public static volatile Queue<DataMessageWrapper> messageQ = new LinkedList<DataMessageWrapper>();
	public static Hashtable<String, Socket> peerIDToSocketMap = new Hashtable<String, Socket>();	 // peerID 와 해당되는 소켓을 매칭한 해시테이블
	public static Vector<Thread> receivingThread = new Vector<Thread>();		// 각 벡터의 값이 스레드. 여러 스레드를 한꺼번에 쉽게 관리하기 위함
	public static Vector<Thread> sendingThread = new Vector<Thread>();
	public static Thread messageProcessor;
	
	
	public static CommonProperties CommonProperties = new CommonProperties();
	
	public static synchronized void addToMsgQueue(DataMessageWrapper msg) {
		messageQ.add(msg);
	}
	
	// synchronized는 쓰레드가 메모리를 공유하는 경우 한 쓰레드가 메모리를 점유하고 있는 동안 다른 쓰레드가 동일 메모리를 변경할 수 없도록 하는 방법
	public static synchronized DataMessageWrapper removeFromMsgQueue() {	// 큐에서 메세지를 꺼냄
		DataMessageWrapper msg = null;
		
		if(!messageQ.isEmpty())	{
			msg = messageQ.remove();
		}
		return msg;
	}

	public static void readPeerInfoAgain() {	// 피어 정보를 다시 읽어옴 (완료되어있는걸 업데이트하면서)
		BufferedReader in = null;
		try {
			String st;
			String[]args = null;
			in = new BufferedReader(new FileReader("PeerInfo.cfg"));
			while ((st = in.readLine()) != null) {
				if (st.trim().length() > 0){
					args = st.trim().split("\\s+");
				
					if (args.length != 4) {
						throw new Exception("peerInfo is Error.");
					}
					
					String peerID = args[0];
					int isCompleted = Integer.parseInt(args[3]);
					if(isCompleted == 1) {
						peerCommonMapUtil.getRemotePeerInfo(peerID).isCompleted = 1;
						peerCommonMapUtil.getRemotePeerInfo(peerID).isInterested = 0;
						peerCommonMapUtil.getRemotePeerInfo(peerID).isChoked = 0;
					}
				} else {
					break;
				}
			}
		} catch (Exception e) {
			showLog(peerID + e.toString());
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				showLog(peerID + e.toString());
			}
		}
	}
	
	// handles the preferred neighbors information
	public static class PreferedNeighbors extends TimerTask {  // 서버는 컨피그에 있는 모든 피어가 파일을 전부 다운로드 완료할때까지 기다리는거네요
		@Override
		public void run() {
			//updates remotePeerInfoHash
			readPeerInfoAgain();
			Enumeration<String> keys = peerCommonMapUtil.getRemotePeerInfoHashKeys();
			int countInterested = 0;
			String strPref = "";
			while(keys.hasMoreElements()) {
				String key = (String)keys.nextElement();
				RemotePeerInfo pref = peerCommonMapUtil.getRemotePeerInfo(key);
				
				if(key.equals(peerID))	// 현재 peerID와 동일한 해시 테이블의 정보는 그냥 넘어간다
					continue;
				
				if (pref.isCompleted == 0 && pref.isHandShaked == 1) {		// interested 한 피어 개수를 카운트
					countInterested++;
				} else if(pref.isCompleted == 1) {		// 완료된 피어는 빼줌
					try	{
						peerCommonMapUtil.preferedNeighborsRemove(key);
					} catch (Exception e) {
					}
				}
			}
			
			if(countInterested > CommonProperties.getNumOfPreferredNeighbr()) {		// 인터레스트 메세지를 보낸 피어가 현재 설정된 프리퍼 네이버 개수보다 많은 경우
				boolean flag = peerCommonMapUtil.isPreferedNeighbors();		// flag 는 현재 prefered 네이버가 없으면 true. 있으면 false 를 리턴
				if(!flag)		// 즉 현재 prefered 네이버가 있는 상태
					peerCommonMapUtil.preferedNeighborsClear();		// 프리퍼 네이버를 초기화
				List<RemotePeerInfo> pv = new ArrayList<RemotePeerInfo>(peerCommonMapUtil.getRemotePeerInfoHashValues());	// 해시테이블에서 value 들을 어레이로 받아옴
				Collections.sort(pv, new PeerDataRateComparator(false));		// 데이터 레잇 기준으로 솔팅함(솔팅 기준은 리모트피어인포에있음). 메세지 프로세서에서 데이터 rate 은 리퀘스트 보내고 타이머 스타트해서 피스 받으면 finish 함
				int count = 0;
				for (int i = 0; i < pv.size(); i++) 
				{
					if (count > CommonProperties.getNumOfPreferredNeighbr() - 1)		// Common.cfg 에서 정의된 수만큼 프리퍼 네이버를 설정
						break;
					if(pv.get(i).isHandShaked == 1 && !pv.get(i).peerId.equals(peerID) 	// 현재 돌아가는 피어의 아이디가 아니면서 핸드세이크를 한경우면서 완료 아직 안된경우
							&& peerCommonMapUtil.getRemotePeerInfo(pv.get(i).peerId).isCompleted == 0)
					{
						peerCommonMapUtil.getRemotePeerInfo(pv.get(i).peerId).isPreferredNeighbor = 1;		// 프리퍼 네이버로 설정
						peerCommonMapUtil.setPreferedNeighbors(pv.get(i).peerId, peerCommonMapUtil.getRemotePeerInfo(pv.get(i).peerId));	// 해시테이블에 저장
						
						count++;
						
						strPref = strPref + pv.get(i).peerId + ", ";
						
						if (peerCommonMapUtil.getRemotePeerInfo(pv.get(i).peerId).isChoked == 1)		// 현재 초크된 피어인 경우
						{
							sendUnChoke(peerProcess.peerIDToSocketMap.get(pv.get(i).peerId), pv.get(i).peerId);		// 언초크 메세지를 보냄
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(pv.get(i).peerId).isChoked = 0;
							sendHave(peerProcess.peerIDToSocketMap.get(pv.get(i).peerId), pv.get(i).peerId);
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(pv.get(i).peerId).state = 3;
						}
						
						
					}
				}
			}
			else		// 인터레스트 메세지를 보낸 피어가 현재 설정된 프리퍼 네이버보다 적은 경우 -> 그 피어들을 프리퍼 네이버로 설정
			{
				keys = peerCommonMapUtil.getRemotePeerInfoHashKeys();
				while(keys.hasMoreElements())
				{
					String key = (String)keys.nextElement();
					RemotePeerInfo pref = peerCommonMapUtil.getRemotePeerInfo(key);
					if(key.equals(peerID)) continue;
					
					if (pref.isCompleted == 0 && pref.isHandShaked == 1)
					{
						if(!peerCommonMapUtil.preferedNeighborsContainsKey(key))
						{
							strPref = strPref + key + ", ";
							peerCommonMapUtil.setPreferedNeighbors(key, peerCommonMapUtil.getRemotePeerInfo(key));
							peerCommonMapUtil.getRemotePeerInfo(key).isPreferredNeighbor = 1;
						}
						if (pref.isChoked == 1)
						{
							sendUnChoke(peerProcess.peerIDToSocketMap.get(key), key);
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(key).isChoked = 0;
							sendHave(peerProcess.peerIDToSocketMap.get(key), key);
							peerProcess.peerCommonMapUtil.getRemotePeerInfo(key).state = 3;
						}
						
					} 
					
				}
			}
			if (strPref != "")
				peerProcess.showLog(peerProcess.peerID + " has selected the preferred neighbors - " + strPref);
		}
	}
	
	private static void sendUnChoke(Socket socket, String remotePeerID) {
		showLog(peerID + " is sending UNCHOKE message to remote Peer " + remotePeerID);
		DataMessage d = new DataMessage(DataMessageType.DATA_MSG_UNCHOKE.getValue());
		byte[] msgByte = DataMessage.encodeMessage(d);
		SendData(socket, msgByte);
	}
	private static void sendHave(Socket socket, String remotePeerID) {
		byte[] encodedBitField = peerProcess.ownBitField.encode();
		showLog(peerID + " sending HAVE message to Peer " + remotePeerID);
		DataMessage d = new DataMessage(DataMessageType.DATA_MSG_HAVE.getValue(), encodedBitField);
		SendData(socket,DataMessage.encodeMessage(d));
		encodedBitField = null;
	}
	private static int SendData(Socket socket, byte[] encodedBitField) {
		OutputStream out = null;
		try {
			out = socket.getOutputStream();
			out.write(encodedBitField);
		} catch (IOException e) {
			showLog(peerID + e.toString());
			return 0;
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				showLog(peerID + e.toString());
			}
		}
		return 1;
	}

	/**
	 * handles the Optimistically unchoked neigbhbors' information
	 * Adding the Optimistically unchoked neighors to the corresponding
	 * list; 
	 */
	public static class UnChokedNeighbors extends TimerTask {

		@Override
		public void run() 
		{
			//updates remotePeerInfoHash
			readPeerInfoAgain();
			if(!peerCommonMapUtil.isUnchokedNeighbors())
				peerCommonMapUtil.unchokedNeighborsClear();
			Enumeration<String> keys = peerCommonMapUtil.getRemotePeerInfoHashKeys();
			Vector<RemotePeerInfo> peers = new Vector<RemotePeerInfo>();
			while(keys.hasMoreElements())
			{
				String key = (String)keys.nextElement();
				RemotePeerInfo pref = peerCommonMapUtil.getRemotePeerInfo(key);
				if (pref.isChoked == 1 		// 현재 초크 되어있는 걸 peers 벡터에 추가함
						&& !key.equals(peerID) 
						&& pref.isCompleted == 0 
						&& pref.isHandShaked == 1)
					peers.add(pref);
			}
			
			// randomize the vector elements 	
			if (peers.size() > 0)
			{
				Collections.shuffle(peers);	// 임의로 섞음
				RemotePeerInfo p = peers.firstElement();
				
				peerCommonMapUtil.getRemotePeerInfo(p.peerId).isOptUnchokedNeighbor = 1;
				peerCommonMapUtil.setUnchokedNeighbors(p.peerId, peerCommonMapUtil.getRemotePeerInfo(p.peerId));
				peerProcess.showLog(peerProcess.peerID + " has the optimistically unchoked neighbor " + p.peerId);
				
				if (peerCommonMapUtil.getRemotePeerInfo(p.peerId).isChoked == 1)
				{
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(p.peerId).isChoked = 0;
					sendUnChoke(peerProcess.peerIDToSocketMap.get(p.peerId), p.peerId);
					sendHave(peerProcess.peerIDToSocketMap.get(p.peerId), p.peerId);		// 임의로 다시 언초크 네이버를 선정해서 언초크와 해브 메세지를 보냄
					peerProcess.peerCommonMapUtil.getRemotePeerInfo(p.peerId).state = 3;	// 초크 풀렸다고 메세지 보내고 스테이트 3으로 설정함.
				}
			}
			
		}

	}

	/**
	 * start and stop the Prefered Neighbors and Optimistically
	 * unchoked neigbhbors update threads
	 */
	public static void startUnChokedNeighbors() 		// 설정된 기간마다 언초크 네이버를 실행시킴
	{
		timerPref = new Timer();
		timerPref.schedule(new UnChokedNeighbors(),
				CommonProperties.getOptUnchokingInterval() * 1000 * 0,
				CommonProperties.getOptUnchokingInterval() * 1000);
	}

	public static void stopUnChokedNeighbors() {
		timerPref.cancel();
	}

	public static void startPreferredNeighbors() {		// 설정된 기간마다 프리퍼네이버 함수를 실행시킴
		timerPref = new Timer();
		timerPref.schedule(new PreferedNeighbors(),
				CommonProperties.getOptUnchokingInterval() * 1000 * 0,
				CommonProperties.getOptUnchokingInterval() * 1000);		// 천이 1초 단위
	}

	public static void stopPreferredNeighbors() {
		timerPref.cancel();
	}

	/**
	 * Generates log message in following format
	 * [Time]: Peer [peer_ID] [message]
	 * @param message
	 */
	public static void showLog(String message)
	{
		LogGenerator.writeLog(DateUtil.getTime() + ": Peer " + message);
		System.out.println(DateUtil.getTime() + ": Peer " + message);
	}
	
	/**
	 * Reads the Peer details from the PeerInfo.cfg file 
	 * and populates to peerInfoVector vector
	 */
	public static void readPeerInfo() {
		String st;
		BufferedReader in = null;
		
		try {
			in = new BufferedReader(new FileReader("PeerInfo.cfg"));   // 컨피그 파일을 버퍼리더로 읽어옴
			int i = 0;
			while ((st = in.readLine()) != null) {    // 엔터키가 있을때까지 한 줄을 읽음 (한줄만 있다면 while문 못빠져나옴), 다음 라인이 존재하면 계속 읽어내려감
				String[] tokens = st.split("\\s+");  // 구분자 공백으로 단어를 쪼개서 token 배열에 담음
				if (tokens.length != 4) {								// PeerID 가 4개의 숫자로 되어있지 않은 경우인듯
					throw new Exception("Error in PeerInfo.cfg"); 
				}
				peerCommonMapUtil.setRemotePeerInfoHash(tokens[0], new RemotePeerInfo(tokens[0],		// token[0] 즉 peerID 에 대해 RemotePeerInfo 즉 아이디, 주소, 포트넘버, 파일가지고있는지여부(0,1로 판단) 을 peerID 에 대응하는 Hashtable 로 저장
						tokens[1], tokens[2], Integer.parseInt(tokens[3]), i));		// i 가 왜 필요하지? -> 이게 token[3] 가 RemotePeerInfo(pIsFirstPeer) 로 들어가고 i 가 index로 들어가는건가
				i++;			// 왜 증가시키는거지 (각 peer 당 index를 할당하는 것인듯)
			}
		} catch (Exception ex) {			// try 안에서 exception 발생 시 실행할 구문
			showLog(peerID + ex.toString());		// toString: 객체가 가진 정보를 문자열로 반환
		} finally {				// 에러가 발생하던 안하던 무조건 실행되는 구문
			try {
				in.close();
			} catch(IOException e){}
		}
	}
	
	public static void main(String[] args) {
		peerProcess pProcess = new peerProcess();
		peerID = args[0];     // 아규 변수 부분에 java peerProcess 1001 이렇게 실행시킬 때 1001 이 들어감

		try {
			if (peerID == null)
				throw new Exception("peerID is empty...");
			
			// starts saving standard output to log file
			LogGenerator.start("log_peer_" + peerID +".log");	// peerID 이름으로 로그를 저장할 파일을 만듬
			showLog(peerID + " is started");	// start 됐다는 메세지를 화면에 띄우면서 동시에 로그에 저장

			// reads PeerInfo.cfg file and populates RemotePeerInfo class
			readPeerInfo();   // 피어인포 파일을 읽고 그 정보를 hashtable 에 저장
			
			// for the initial calculation
			initializePrefferedNeighbours();
			
			boolean isFirstPeer = false;

			Enumeration<String> e = peerCommonMapUtil.getRemotePeerInfoHashKeys();
			
			while(e.hasMoreElements())
			{
				RemotePeerInfo peerInfo = peerCommonMapUtil.getRemotePeerInfo(e.nextElement());		// 해쉬테이블에서 peerInfo 정보 가져옴. 초기에 정보 가져올때는 isFirstPeer는 구분이 안되있는건가?
				if(peerInfo.peerId.equals(peerID))	// 실행할때 아규먼트로 받은 peerID 와 해쉬테이블에 있는 peerID 와 매칭
				{
					// checks if the peer is the first peer or not
					pProcess.LISTENING_PORT = Integer.parseInt(peerInfo.peerPort);	// Listening Port 에 해당 아규먼트와 일치하는 Port 넘버 할당
					pProcess.myPeerIndex = peerInfo.peerIndex;		// myPeerIndex에 peerIndex 을 할당
					if(peerInfo.getIsFirstPeer() == 1)    // 퍼스트피어 값이 언제 할당되지?(처음에 컨피그에 hasfile 필드인듯) 첫번째 피어이면 서버가 됨 (첫번째이면서 컨피그 파일에 1로 되어잇어야 서버가 되는걸로 추정됨)
					{
						isFirstPeer = true;		// 컨피그에 hasfile 필드에 1로 되어있으면 isFirstPeer로 설정
						break;	// break 가 있어서 firstpeer 가 한대만 되게 됨
					}
				}
			}
			
			// Initialize the Bit field class 
			ownBitField = new BitField();
			ownBitField.initOwnBitfield(peerID, isFirstPeer?1:0);	// isFirstPeer 가 True 면 1, False 면 0이 들어감
			
			messageProcessor = new Thread(new MessageProcessor(peerID));   // 각 피어에서 메세지 프로세서 스레드를 생성
			messageProcessor.start();		// 메세지 프로세서 스레드 실행
			
			if(isFirstPeer)
			{
				try
				{
					pProcess.listeningSocket = new ServerSocket(pProcess.LISTENING_PORT);   // isFirstPeer 가 true 이면 서버 소켓을 생성, 접속을 기다림(연결수락 담당)
					
					//instantiates and starts Listening Thread
					pProcess.listeningThread = new Thread(new ListeningThread(pProcess.listeningSocket, peerID));   // 리스닝 프로세서 스레드 만듬. 서버로 연결 기다리고, 내부에 remote handler 스레드로 메세지 주고 받음
					pProcess.listeningThread.start();
				}
				catch(SocketTimeoutException tox)
				{
					showLog(peerID + " gets time out expetion: " + tox.toString());
					LogGenerator.stop();
					System.exit(0);
				}
				catch(IOException ex)
				{
					showLog(peerID + " gets exception in Starting Listening thread: " + pProcess.LISTENING_PORT + ex.toString());
					LogGenerator.stop();
					System.exit(0);
				}
			}
			// Not the first peer
			else
			{	
				createEmptyFile();
				
				e = peerCommonMapUtil.getRemotePeerInfoHashKeys();
				while(e.hasMoreElements())
				{
					RemotePeerInfo peerInfo = peerCommonMapUtil.getRemotePeerInfo(e.nextElement());
					if(pProcess.myPeerIndex > peerInfo.peerIndex)		// 나보다 이전 피어를 위한 리모트핸들러 스레드를 생성
					{
						Thread tempThread = new Thread(new RemotePeerHandler(			// 나보다 index 작은 peer 의 개수만큼 RemotePeerHandler Thread 를 만들어줌
								peerInfo.getPeerAddress(), Integer.parseInt(peerInfo.getPeerPort()), 1,
								peerID));
						receivingThread.add(tempThread);
						tempThread.start();
					}
				}

				// Spawns a listening thread
				try
				{
					pProcess.listeningSocket = new ServerSocket(pProcess.LISTENING_PORT);		// 현재 피어 이후 생성될 피어들을 위한 리스닝 스레드를 생성해놓음
					pProcess.listeningThread = new Thread(new ListeningThread(pProcess.listeningSocket, peerID));
					pProcess.listeningThread.start();
				}
				catch(SocketTimeoutException tox)
				{
					showLog(peerID + " gets time out exception in Starting the listening thread: " + tox.toString());
					LogGenerator.stop();
					System.exit(0);
				}
				catch(IOException ex)
				{
					showLog(peerID + " gets exception in Starting the listening thread: " + pProcess.LISTENING_PORT + " "+ ex.toString());
					LogGenerator.stop();
					System.exit(0);
				}
			}
			
			startPreferredNeighbors();	// 타이머 스레드 생성해서 씀, 세팅된 시간마다 프리퍼 네이버 선정
			startUnChokedNeighbors();   // 타이머 스레드 생성해서 씀, 세팅된 시간마다 언초크 네이버 생성
			
			while(true)
			{
				// checks for termination
				isFinished = isFinished();    // isFinished() 이 메소드가 다운이 전부 다 끝났는지 체크하는거에요
				if (isFinished) {	// 전부 완료된 경우 -> 스레드 종료 시킴
					showLog("All peers have completed downloading the file.");

					stopPreferredNeighbors();
					stopUnChokedNeighbors();

					try {
						Thread.currentThread();
						Thread.sleep(2000);		// 스레드 들 종료 시킴
					} catch (InterruptedException ex) {
					}

					if (pProcess.listeningThread.isAlive())
						pProcess.listeningThread.stop();

					if (messageProcessor.isAlive())
						messageProcessor.stop();

					for (int i = 0; i < receivingThread.size(); i++)
						if (receivingThread.get(i).isAlive())
							receivingThread.get(i).stop();

					for (int i = 0; i < sendingThread.size(); i++)
						if (sendingThread.get(i).isAlive())
							sendingThread.get(i).stop();

					break;
				} else {
					try {
						Thread.currentThread();
						Thread.sleep(5000);
					} catch (InterruptedException ex) {
					}
				}
			}
		}
		catch(Exception ex)
		{
			showLog(peerID + " Exception in ending : " + ex.getMessage() );
		}
		finally
		{
			showLog(peerID + " Peer process is exiting..");
			LogGenerator.stop();
			System.exit(0);
		}
	}

	private static void initializePrefferedNeighbours() 
	{
		Enumeration<String> keys = peerCommonMapUtil.getRemotePeerInfoHashKeys();
		while(keys.hasMoreElements())		// hasMoreElement: 커서 바로 앞에 데이터가 들어가 있는지 체크, 있으면 true를 리턴
		{
			String key = (String)keys.nextElement();	// 현재 커서가 가리키고 있는 데이터를 리턴해주고 커서를 다음 칸으로 옮김
			if(!key.equals(peerID))
			{
				peerCommonMapUtil.setPreferedNeighbors(key, peerCommonMapUtil.getRemotePeerInfo(key));		// remotePeerInfoHash 의 key와 RemotePeerInfo 를 preferedNeighbors 해시테이블에 저장
			}
		}
	}

	/**
	 * Checks if all peer has down loaded the file
	 */
	public static synchronized boolean isFinished() {		// 파일 다받아졌는지 체크. 메세지 프로세서에서 파일을 받으면 컨피그 업데이트하니까 여기서 hasfile 필드보고 확인

		String line;
		int hasFileCount = 1;
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(
					"PeerInfo.cfg"));

			while ((line = in.readLine()) != null) {
				hasFileCount = hasFileCount
						* Integer.parseInt(line.trim().split("\\s+")[3]);
			}
			if (hasFileCount == 0) {
				in.close();
				return false;
			} else {
				in.close();
				return true;
			}

		} catch (Exception e) {
			showLog(e.toString());
			return false;
		}
	}
	
	public static void createEmptyFile() {		// 다운 받기 위한 엠티 파일 생성
		OutputStream os = null;
		try {
			File dir = new File(peerID);

			if (!dir.isDirectory()) {
				dir.mkdir();
				showLog(peerID + " Make is Directory");
			}
			
			File newfile = new File(peerID, CommonProperties.getFileName());
			os = new FileOutputStream(newfile, true);
			
			byte b = 0;
			for (int i = 0; i < CommonProperties.getFileSize(); i++)
				os.write(b);		// 0 byte 로 파일을 채움

		} catch (Exception e) {
			showLog(peerID + " ERROR in creating the file : " + e.getMessage());
		} finally {
			try {
				os.close();
			} catch(IOException e){}
		}
	}
}

