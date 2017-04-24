package com.example.test.ComAssistant;


public class Java_xor {

	public static String checkcode_0007(String para){  
		int len = para.length()/2 ;
        String[] dateArr = new String[len];  
        try { 
        	for (int i = 0; i < len; i++) {
        		 dateArr[i] = para.substring(2*i, 2*i+2);  
			}
       } catch (Exception e) {  
           // TODO: handle exception  
       }  
       String code = "00000000";  
       for (int i = 0; i < dateArr.length; i++) {  
               code = xor(code, dateArr[i]);  
       }  
       if(code.length()==1){
    	   code = "0"+code;
       }
       return code;  
}  
	
	public static String xor(String strHex_X,String strHex_Y){   
	        //将x、y转成二进制形�?   
	        String oneString=Integer.toBinaryString(Integer.valueOf(strHex_X,16));  
	        String twoString=Integer.toBinaryString(Integer.valueOf(strHex_Y,16));   
	        String result = "";   
	        //判断是否�?8位二进制，否则左补零   
	        if(oneString.length() != 8){   
	        for (int i = oneString.length(); i <8; i++) {   
	        	oneString = "0"+oneString; 
	            }   
	        }   
	        if(twoString.length() != 8){   
	        for (int i = twoString.length(); i <8; i++) {   
	        	twoString = "0"+twoString;   
	            }   
	        }   
	        //异或运算   
	        for(int i=0;i<oneString.length();i++){   
	        //如果相同位置数相同，则补0，否则补1   
	                if(twoString.charAt(i)==oneString.charAt(i))   
	                    result+="0";   
	                else{   
	                    result+="1";   
	                }   
	            }  
	        return Integer.toHexString(Integer.parseInt(result, 2));   
	    }  

}
