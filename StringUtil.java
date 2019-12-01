
public class StringUtil {

	public static final String defaultString(String str, String defaultStr){		// 문자열 값이 null 이면 "" 를 반환하고 값이 있으면 해당 값 반환(nullPointerException 에러 발생 안시킴)
		String value = null;
		if (isEmpty(str)) {
			value = defaultStr;
		} else {
			value = str;
		}
		return value;
	}
	
	public static final boolean isEmpty(Object obj) {					// null 인지 길이가 0인지 확인하고 반환
		if (obj == null) {
			return true;
		} else if ("".equals(obj)) {
			return true;
		} else if (obj instanceof Object[]) {
			for (Object v : ((Object[])obj)) {
				if (!isEmpty(v)) return false;
			}
			return true;
		} else 
			return false;
	}  
}
