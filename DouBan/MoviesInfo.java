package douBan;

public class MoviesInfo {
    String cnName;//中文名
    String frName;//外文名
    String tbName;//台湾名
    String hkName;//香港名
    String canPlay = "可播放";//是否可播放
    String director;//导演名
    String mainActor;//主演名
    String country;//国家名
    String psYear;//上映年份
    String mvType;//影片类型
    String score;//评价星级
    String cmNum;//评价人数
    String comment;//评语

    @Override
    public String toString() {
        return "中文名：" + cnName + "  " + "外文名：" + frName + "  "
                + "香港名：" + hkName + "  " + "台湾名：" + tbName + "  "
                + "是否可播放：" + canPlay + "  " +"导演名：" + director + "  " + "演员名：" + mainActor + "  "
                + "国家：" + country + "  " + "类型：" + mvType + "  "
                + "上映年份：" + psYear + "  " + "星级：" + score + "  "
                + "  " + "评价人数：" + cmNum + "  " + "评价：" + comment;
    }
}