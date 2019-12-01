import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;

public class PeerCommonMapUtil {

	private Hashtable<String, RemotePeerInfo> remotePeerInfoHash;
	private Hashtable<String, RemotePeerInfo> preferedNeighbors;
	private Hashtable<String, RemotePeerInfo> unchokedNeighbors;
	
	public PeerCommonMapUtil() {
		this.remotePeerInfoHash = null;  //전역변수로 remotePeerInfoHash 선언되어 있음:  전역변수는 현재 클래스 내에서 공통으로 사용할수있는 변수를 의미하는거고, 지역변수는 메소드내에 선언되어서 메소드 내에서만 사용할수 있는걸 말해요
		this.preferedNeighbors = null;
		this.unchokedNeighbors = null;
	}
	
	public void setRemotePeerInfoHash(String key, RemotePeerInfo value) {   // key(PeerID), value(PeerID, 주소, 포트넘버, isFirst) 형태로 hashtable 에 저장함  (한줄씩 작성된 컨피크 파일의 피어정보들이 담겨지게 됨)
		if (this.remotePeerInfoHash == null) {			// Hashtable 이 없으면 해쉬테이블 생성
			this.remotePeerInfoHash = new Hashtable<String, RemotePeerInfo>();          // this. 은 말그대로 현재 클래스를 의미
		}
		
		this.remotePeerInfoHash.put(key, value);			// Hashtable 에 값을 넣어줌
	}
	
	public RemotePeerInfo getRemotePeerInfo(String key) {
		if (StringUtil.isEmpty(key)) {				// Hashtable 이 비어있으면 null 을 return
			return null;
		}
		
		if (this.remotePeerInfoHash == null) {
			this.remotePeerInfoHash = new Hashtable<String, RemotePeerInfo>();		// Hashtable 이 비어있으면 새로 생성
		}
		
		RemotePeerInfo remotePeerInfo = null;
		
		if (this.remotePeerInfoHash != null && this.remotePeerInfoHash.size() > 0) {	// Hashtable 이 비어있지 않으면 값을 가져옴
			remotePeerInfo = this.remotePeerInfoHash.get(key);		// 해당 key 에 대한 value 값을 가져와서 remotePeerInfo 에 저장
		}
		
		return remotePeerInfo;
	}
	
	public Enumeration<String> getRemotePeerInfoHashKeys() {		// key 값만 넘겨주기 위한 함수
		return this.remotePeerInfoHash.keys();	// key 값을 리턴
	}
	
	public Collection<RemotePeerInfo> getRemotePeerInfoHashValues() {
		return remotePeerInfoHash.values();		// value 값을 리턴
	}
	
	// preferedNeighbors 에는 실제 연결된 피어 정보를 저장하는 해시 테이블
	public void setPreferedNeighbors(String key, RemotePeerInfo value) {
		if (this.preferedNeighbors == null) {
			this.preferedNeighbors = new Hashtable<String, RemotePeerInfo>();
		}
		
		this.preferedNeighbors.put(key, value);		// 프리퍼 네이버 정보를 해시테이블에 저장
	}
	
	public RemotePeerInfo getPreferedNeighbors(String key) {
		if (StringUtil.isEmpty(key)) {
			return null;
		}
		
		if (this.preferedNeighbors == null) {
			this.preferedNeighbors = new Hashtable<String, RemotePeerInfo>();
		}
		
		RemotePeerInfo remotePeerInfo = null;
		
		if (this.preferedNeighbors != null && this.preferedNeighbors.size() > 0) {
			remotePeerInfo = this.preferedNeighbors.get(key);
		}
		
		return remotePeerInfo;
	}
	
	public RemotePeerInfo preferedNeighborsRemove(String key) {
		return this.preferedNeighbors.remove(key);
	}
	
	public boolean isPreferedNeighbors() {
		return this.preferedNeighbors.isEmpty();
	}
	
	public void preferedNeighborsClear() {
		this.preferedNeighbors.clear();
	}
	
	public boolean preferedNeighborsContainsKey(String key) {		// 해당 key 가 perferedNeighbor 인지 혹인
		return this.preferedNeighbors.containsKey(key);
	}
	
	public void setUnchokedNeighbors(String key, RemotePeerInfo value) {
		if (this.unchokedNeighbors == null) {
			this.unchokedNeighbors = new Hashtable<String, RemotePeerInfo>();
		}
		
		this.unchokedNeighbors.put(key, value);
	}
	
	public RemotePeerInfo getUnchokedNeighbors(String key) {
		if (StringUtil.isEmpty(key)) {
			return null;
		}
		
		if (this.unchokedNeighbors == null) {
			this.unchokedNeighbors = new Hashtable<String, RemotePeerInfo>();
		}
		
		RemotePeerInfo remotePeerInfo = null;
		
		if (this.unchokedNeighbors != null && this.unchokedNeighbors.size() > 0) {
			remotePeerInfo = this.unchokedNeighbors.get(key);
		}
		
		return remotePeerInfo;
	}
	
	public boolean isUnchokedNeighbors() {
		if (this.unchokedNeighbors == null) {
			return true;
		}
		return this.unchokedNeighbors.isEmpty();
	}
	
	public void unchokedNeighborsClear() {
		this.unchokedNeighbors.clear();
	}
	
	public boolean unchokedNeighborsContainsKey(String key) {
		return this.unchokedNeighbors.containsKey(key);
	}
	
}
