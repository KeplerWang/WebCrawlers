package douBan;

import java.sql.*;
import java.util.Scanner;
import java.util.regex.*;
import java.io.IOException;

import com.mysql.cj.x.protobuf.MysqlxDatatypes;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DouBanWebCrawler {
    String url = "https://movie.douban.com/top250?start=0&filter=";//默认第一页 获取25个
    int num=25;
    public DouBanWebCrawler(String url,int num){
        this.url=url;
        this.num=num;
    }
    public DouBanWebCrawler(String url){
        this.url=url;
    }
    public DouBanWebCrawler(int num){
        this.num=num;
    }
    public DouBanWebCrawler(){
        super();
    }

    public void getInformation()throws Exception{
        int number;
        if(num%25==0)
            number=num/25;
        else number=num/25+1;
        MoviesInfo[] mvif=new MoviesInfo[number*25];
        for(int q=0;q<number*25;q++)
            mvif[q]=new MoviesInfo();
        for(int i=1;i<=number;i++) {
            String[] temp=url.split("=");
            String myurl=temp[0]+"="+((i-1)*25)+"&filter=";
            getInformationTool(myurl, mvif, (i - 1) * 25, i * 24);
        }
        System.out.print("是否写入数据库Y/N？");
        Scanner in=new Scanner(System.in);
        if(in.nextLine().equals("Y"))
            writeToDataBase(mvif,0,num-1);
        else
            System.out.println("好的，我什么都没有做....");
    }

    //获取信息工具
    private void getInformationTool(String url,MoviesInfo[] mvif,int start,int end) throws Exception{
        Document document = Jsoup.connect(url).timeout(3000).get();
        Elements names = document.getElementsByAttributeValue("class", "hd"); //电影名字和是否可播放
        Elements scores = document.getElementsByAttributeValue("class", "star").select("[property=\"v:average\"]");//// 电影星级
        Elements bd=document.getElementsByAttributeValue("class","bd");//评价
        Elements stars = document.getElementsByAttributeValue("class", "star");//评价人数
        Elements otherInfos = document.getElementsByAttributeValue("class", " ");//电影导演 主演 年代 国家 类型

        int k = start, j = start, l = start, h = start, o = start,x=start;
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        Pattern p = Pattern.compile("(^[\\u4E00-\\u9FFF])(.?)+$");//匹配中文

        //获取评价人数
        for (Element e : stars) {
            mvif[k] = new MoviesInfo();
            mvif[k++].cmNum = e.text().split(" ")[1].split("人评价")[0];
        }

        //获取评语
        for(Element e:bd) {
            if (e.toString().contains("导演")) {
                if (e.toString().contains("quote"))
                    mvif[x++].comment=e.getElementsByAttributeValue("class","quote").text();
                else
                    mvif[x++].comment=null;
            }
        }


        for (Element e : otherInfos) {
            if (e.text().contains("导演")) {
                String[] tempDirNames=e.text().split("主演|&")[0].split(" |/|[a-zA-Z]|导演:|导演：|-|[\\u0100-\\u017F]");//导演名
                String[] tempPsYears = e.text().split("/ ");//年份 国家 类型

                if(e.text().contains("主演")||e.text().contains("主")) {
                    String[] tempActors = e.text().split("主演:|(主...)|主演：")[1].split("([0-2])(.*)")[0].split(" |/");
                    for (String s : tempActors) {
                        if (!s.equals("")) {
                            Matcher m = p.matcher(s);
                            if (m.find())
                                sb2.append(s + " ");
                            if (s.contains("...") && !sb2.toString().contains("..."))
                                sb2.append("...");
                        }
                    }
                }
                if (sb2.toString().equals(""))
                    sb2.append("...");



                for (String s : tempDirNames) {
                    if(!s.isBlank())
                        sb1.append(s+" ");
                }

                mvif[o++].mainActor = sb2.toString();
                mvif[h].country = tempPsYears[tempPsYears.length - 2];
                mvif[h++].mvType = tempPsYears[tempPsYears.length - 1];
                mvif[j++].director=sb1.toString().isBlank()?null: sb1.toString();
                sb1=new StringBuilder();
                sb2 = new StringBuilder();

                for (String s : tempPsYears) {
                    String[] ttemp;
                    if (s.contains("1") || s.contains("2")) {
                        ttemp = s.split(" ");
                        mvif[l++].psYear = ttemp[ttemp.length - 1];
                    }
                }
            }
        }

        for (int i = 0; i < names.size(); i++) {
            Element tempName = names.get(i);
            Element tempScore = scores.get(i);


            String score = tempScore.text();
            String[] name = tempName.text().split(" / |\\  |\\[|\\]");

            mvif[i+start].score = score;
            mvif[i+start].cnName = name[0];

            int q, tag = 1;
            for (q = 1; q < name.length; q++) {
                if (name[q].contains("(台)") || name[q].contains("(港/台)") || name[q].contains("(台/港)")) {
                    mvif[i+start].tbName = name[q].split("\\(台\\)|\\(港/台\\)|\\(台/港\\)")[0];
                } else if (name[q].contains("(港)") || name[q].contains("(港/台)") || name[q].contains("(台/港)")) {
                    mvif[i+start].hkName = name[q].split("\\(港\\)|\\(港/台\\)|\\(台/港\\)")[0];
                } else if ((!isChinese(name[q])) && (tag != 0)) {
                    mvif[i+start].frName = name[q];
                    tag = 0;
                }
            }
            if (!name[name.length - 1].equals("可播放"))
                mvif[i+start].canPlay = "不可播放";
        }
    }

    // 数据库写入
    private void writeToDataBase(MoviesInfo[] mvif,int startIndex,int endIndex) throws Exception{
        Class.forName("com.mysql.cj.jdbc.Driver");
	//XXXXX分别为 库名 用户名 密码 表明
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/XXXXX?characterEncoding=utf8&useSSL=false&serverTimezone=UTC", "XXXXXXX", "XXXXXXX");
        String sql = "insert into XXXXXXX(中文名,外文名,香港名,台湾名,是否可播放,导演,主演,上映年份,国家,影片类型,评价星级,评价人数,电影评语) values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement st = (PreparedStatement) conn.prepareStatement(sql);
        for (int s = startIndex; s <= endIndex; s++) {
            MoviesInfo temp = mvif[s];
            st.setString(1, temp.cnName);
            st.setString(2, temp.frName);
            st.setString(3, temp.hkName);
            st.setString(4, temp.tbName);
            st.setString(5, temp.canPlay);
            st.setString(6, temp.director);
            st.setString(7, temp.mainActor);
            st.setString(8, temp.psYear);
            st.setString(9, temp.country);
            st.setString(10, temp.mvType);
            st.setString(11, temp.score);
            st.setString(12, temp.cmNum);
            st.setString(13, temp.comment);
            st.executeUpdate();

        }
    }

    private static boolean isChinese(String str){
        String[] strs=str.split(",|，|-|‘|“|’|”|'|·| ");
        StringBuffer sb=new StringBuffer();
        for (String s:strs) {
            sb.append(s);
        }
        str=sb.toString();
        Pattern  P= Pattern.compile("^[\\u4E00-\\u9FA5\\uF900-\\uFA2D]+$");
        Pattern p = Pattern.compile("(^[\\u4E00-\\u9FFF])(.?)+$");
        Matcher M = P.matcher(str);
        Matcher m = p.matcher(str);
        if(m.find()&&M.find())
            return true;
        else
            return false;
    }

    public static boolean isEnglish(String charaString){
       return charaString.matches("(^[a-zA-Z]*)(.*)");
    }
}
