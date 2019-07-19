package Bilibili;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class BiliWebCrawler {
    private int num=30;//抓取条数 默认30个 1页
    private String id="258150656";//Up的ID 默认为回形针Paperclip

    public BiliWebCrawler(int num,String id){
        if(num<=0)
            this.num=30;
        else
            this.num=num;
        this.id=id;
    }

    public BiliWebCrawler(String id){
        this.id=id;
    }

    public BiliWebCrawler(int num){
        if(num<=0)
            this.num=30;
        else
            this.num=num;
    }

    public BiliWebCrawler(){
        super();
    }

    public void getData()throws Exception{
        int endPage;
        if(num%30==0)
            endPage=num/30;
        else
            endPage=num/30+1;

        VideosInfo[] voif=new VideosInfo[endPage*30];
        for(int i=0;i<endPage*30;i++)
            voif[i]=new VideosInfo();

        int flag=-1;
        for(int startPage=1;startPage<=endPage&&flag==-1;startPage++) {
            String url = "https://space.bilibili.com/ajax/member/getSubmitVideos?mid="+id+"&pagesize=30&tid=0&page=" + startPage + "&keyword=&order=pubdate";
            // https://space.bilibili.com/ajax/member/getSubmitVideos?mid=258150656&pagesize=30&tid=0&page=1&keyword=&order=pubdate"
            int temp=startPage*30-1>num-1?num-1:startPage*30-1;
            flag=getDataTool(voif,(startPage-1)*30,temp,url);
        }


        System.out.print("是否写入数据库Y/N？");
        Scanner in=new Scanner(System.in);
        if(in.nextLine().equals("Y")) {
            int number = num;
            if (flag != -1 && flag != -2)
                number = flag;
            writeToDataBase(voif, 0, number-1);
        }
        else
            System.out.println("好的，我什么都没有做....");
    }

    private int getDataTool(VideosInfo[] voif,int startIndex,int endIndex,String url)throws Exception{

        StringBuffer json = new StringBuffer();
        URLConnection UC = new URL(url).openConnection();
        BufferedReader BR = new BufferedReader(new InputStreamReader(UC.getInputStream(), "UTF-8"));
        String inputline = null;
        while ((inputline = BR.readLine()) != null) {
            json.append(inputline);
        }
        BR.close();

        JSONObject joTemp = JSON.parseObject(json.toString()).getJSONObject("data");
        JSONArray jsonArray=joTemp.getJSONArray("vlist");

        int count=joTemp.getInteger("count");
        int flag=-1;
        if(startIndex+1>count)
            return -2;
        else if(endIndex+1>count) {
            endIndex = count-1;
            flag=count;
        }
        for(int i=startIndex;i<=endIndex;i++){
            JSONObject jo=jsonArray.getJSONObject(i-startIndex);
            voif[i].title=jo.getString("title");
            voif[i].subTitle=jo.getString("subtitle");
            voif[i].createdTime=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(jo.getBigInteger("created").toString())*1000));
            voif[i].description=jo.getString("description");
            voif[i].videoLength=jo.getString("length");
            voif[i].picLocation="/home/kepler/Pictures/"+ voif[i].title+".jpg";

            String picUrl="https:"+jo.getString("pic");
            HttpURLConnection huc1 = (HttpURLConnection) new URL(picUrl).openConnection();
            huc1.connect();
            StringBuilder stringBuilder = new StringBuilder();
            if(voif[i].title.contains("/")) {
                for (String s : voif[i].title.split("/|\\.")) {
                    stringBuilder.append(s);
                }
            }
            FileOutputStream fos=new FileOutputStream("/home/kepler/Pictures/"+stringBuilder.toString()+".jpg");
            fos.write(huc1.getInputStream().readAllBytes());
            fos.close();
            huc1.disconnect();

            Thread.sleep(1000);

            int aid = Integer.parseInt(jo.getString("aid"));
            // "https://api.bilibili.com/x/web-interface/archive/stat?aid=59320630"

            URLConnection uc = (URLConnection) new URL("https://api.bilibili.com/x/web-interface/archive/stat?aid="+aid).openConnection();
            StringBuffer sb = new StringBuffer();
            BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            JSONObject jb = JSON.parseObject(sb.toString()).getJSONObject("data");
            voif[i].replyNum=jb.getInteger("reply");
            voif[i].coinNum=jb.getInteger("coin");
            voif[i].shareNum=jb.getInteger("share");
            voif[i].likeNum=jb.getInteger("like");
            voif[i].favoriteNum=jb.getInteger("favorite");
        }
        return flag;
    }

    private void writeToDataBase(VideosInfo[] voif,int startIndex,int endIndex) throws Exception{
        Class.forName("com.mysql.cj.jdbc.Driver");
        //XXXXX分别为 库名 用户名 密码 表名
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/XXXXXX?characterEncoding=utf8&useSSL=false&serverTimezone=UTC", "XXXX", "XXXXX");
        String sql = "insert into XXXXXXXX(标题,副标题,发布时间,描述,长度,封面图片路径,投币数,点赞数,收藏数,分享数,回复数) values(?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement st = (PreparedStatement) conn.prepareStatement(sql);
        for (int s = startIndex; s <= endIndex; s++) {
            VideosInfo temp = voif[s];
            st.setString(1, temp.title);
            st.setString(2, temp.subTitle);
            st.setString(3, temp.createdTime);
            st.setString(4, temp.description);
            st.setString(5, temp.videoLength);
            st.setString(6, temp.picLocation);
            st.setLong(7, Long.parseLong(Integer.toString(temp.coinNum)));
            st.setLong(8, Long.parseLong(Integer.toString(temp.likeNum)));
            st.setLong(9, Long.parseLong(Integer.toString(temp.favoriteNum)));
            st.setLong(10, Long.parseLong(Integer.toString(temp.shareNum)));
            st.setLong(11, Long.parseLong(Integer.toString(temp.replyNum)));
            st.executeUpdate();
        }
    }
}
