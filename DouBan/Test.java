package douBan;

public class Test {
    public static void main(String[] args)throws Exception{
        new DouBanWebCrawler(249).getInformation();
        // num>250或<=0时 默认为25
	//使用须知：在DouBanWebCrawler.java中修改 库名 表名 用户名 密码
        //导入两个jar包 执行Test即可
        //支持指定爬的行数<=250 起始页
        //默认25条 从第一页开始爬

    }
}
