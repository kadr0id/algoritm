package scripting.data.entity;

/**
 *
 * @author GOD
 */
public enum ProjectType {
    //First(""),

    RSS(15, "RSS"),
    PressRelease(14, "Press Release"),
    GooglePlace(13, "Google Place"),
    PDF(12, "PDF"),
    Webprofile(11, "Web 2.0 Profile"),
    WikiMedia(10, "Wiki Media"),
    WikiDoku(50, "Wiki Doku"),
    WikiMacOS(51, "Wiki MacOS"),
    WikiTiki(52, "WikiTiki"),
    WikiWikka(53, "WikiWikka"),
    WikiMoinMoin(54, "WikiMoinMoin"),
    Wiki(9, "Wiki"),
    ArticlePHPLD(26, "Article PHP LD"),
    ArticleWPA(25, "Article WPA"),
    ArticleMS(24, "Article MS"),
    ArticleFriendly(23, "Article Friendly"),
    ArticleDashboard(22, "Article Dashboard"),
    ArticleBeach(21, "Article Beach"),
    ArticleDirectory(20, "Article Directory"),
    BookmarkingPligg(6, "Bookmarking Pligg"),
    BookmarkingElgg(7, "Bookmarking Elgg"),
    BookmarkingDrupal(8, "Bookmarking Drupal"),
    BookmarkingIndonesia(70, "Bookmarking Indonesia"),
    NicheResearch(5, "Niche Research"),
    AccountCreation(4, "Account Creation"),
    AccountProfile(3, "Account Profile"),
    SocialBlogsPHPFox(31, "Social Blogs PHPFox"),
    SocialBlogsPHPIzbi(32, "Social Blogs PHPIzbi"),
    SocialBlogsDolphin(33, "Social Blogs Dolphin"),
    SocialBlogsBuddypress(34, "Social Blogs Buddypress"),
    SocialBlogsWordpress(35, "Social Blogs Wordpress"),
    SocialBlogsElgg(36, "Social Blogs Elgg"),
    SocialBlogsPHPMotion(37, "Social Blogs PHPMotion"),
    SocialBlogsZF(38, "Social Blogs ZF"),
    SocialBlogsJcow(39, "Social Blogs Jcow"),
    SocialBlogsDrupal(40, "Social Blogs Drupal"),
    SocialBlogsPHPLD(41, "Social Blogs PHPLD"),
    SocialBlogsPHPLDNoReg(42, "Social Blogs PHPLD NoReg"),
    SocialBlogsEngine(71, "SocialBlogsEngine"),
    SocialNetwork(2, "Social Network"),
    SocialBookmark(1, "Social Bookmark"),
    ArticleWordPress(100, "Article WordPress"),
    ArticleScript(101, "Article Script"),
    All(99, "All"),
    Mail(0, "Mail"),
    ForumDataLifeEngine(60, "ForumDataLifeEngine"),
    ForumXoops(61, "ForumXoops"),
    ForumPHPNuke(62, "ForumPHPNuke"),
    ForumExpressEngine(65, "ForumExpressEngine"),
    ForumSeoBoard(66, "ForumSeoBoard"),
    ForumFluxBB(69, "ForumFluxBB"),
    WebProfileMoodle(63, "WebProfileMoodle"),
    WebProfilePlone(64, "WebProfilePlone"),
    WebProfileElgg(66, "WebProfileElgg"),
    WebProfileDolphin(72, "WebProfileDolphin"),
    WebProfilePHPFox(73, "WebProfilePHPFox"),
    WebProfileKoRo1(102, "WebProfileKoRo1"),
    Geeklog(41, "Geeklog"),
    PHPFusion(42, "PHPFusion"),
    ForumVanilla(74, "ForumVanilla"),
    ForumBurningBoard(75, "ForumBurningBoard"),
    ForumFreePHPMessageBoard(76, "ForumFeePHPMessageBoard"),
    ForumPHPBB(77, "ForumPHPBB"),
    ForumPunBB(78, "ForumPunBB"),
    ForumSMF(79, "ForumSMF"),
    ForumIndexU(80, "ForumIndexU"),
    ForumElgg(81, "ForumElgg"),
    ForumPHPFox(82, "ForumPHPFox"),
    SocialBlogsZenDesk(83, "SocialBLogsZenDesk"),
    SocialBlogsVldPersonals(84, "SocialBlogsVldPersonals"),
    SocialBlogsEsoTalk(85,"SocialBlogsEsoTalk"),
    SocialBlogs247Bloggama(86,"SocialBlogs247Bloggama"),
    SocialBlogs213SosBlogs(87,"SocialBlogs213SosBlogs"),
    WebProfileVideo(88,"WebProfileVideo"),
    BookmarkingGempixel(89,"BookmarkingGempixel"),
    SocialBlogsBoinc(90,"SocialBlogsBoinc"),
    WebProfileBoinc(91,"WebProfileBoinc"),
    WebProfileClassiPress(92,"WebProfileClassiPress"),
    WebProfileXenforo(93,"WebProfileXenforo"),
    ForumAVArcade(94,"ForumAVArcade"),
    Oxwall(43, "Oxwall"),
    SocialBlogsDiigo(103, "Diigo");
    //Last("");
    private String value = "";
    private int intValue;

    ProjectType(int intValue, String value) {
        this.intValue = intValue;
        this.value = value;
    }

    public String toString() {
        return this.value;
    }

    public int getIntValue() {
        return this.intValue;
    }
}
