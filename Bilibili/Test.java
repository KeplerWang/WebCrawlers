package Bilibili;


public class Test {
    public static void main(String[] args) throws Exception {
        new BiliWebCrawler(30,"10835521").getData();
	// num<=0时 默认为30 超过时自动为最大
	//使用须知：在BiliBiliWebCrawler.java中修改 库名 表名 用户名 密码
        //导入三个jar包 执行Test即可
        //支持指定爬的条数
        //默认30条
    }
}
