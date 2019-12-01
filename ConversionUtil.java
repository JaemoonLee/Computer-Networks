public class ConversionUtil {

	public static byte[] intToByteArray(int value) {			// 바이트어레이로 변경. 네트워크로 전송할때는 바이트어레이로
		byte[] byteArray = new byte[4];
		byteArray[0] = (byte)(value >> 24);
		byteArray[1] = (byte)(value >> 16);
		byteArray[2] = (byte)(value >> 8);
		byteArray[3] = (byte)(value);
		return byteArray;
    }
	
    public static int byteArrayToInt(byte[] arr) {			// 바이트어레이를 다시 int 로 변경
    	return (arr[0] & 0xff)<<24 | (arr[1] & 0xff)<<16 | (arr[2] & 0xff)<<8 | (arr[3] & 0xff);
    }
}