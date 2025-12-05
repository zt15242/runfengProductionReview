// ==UserScript==
// @name         上海开放大学多功能脚本
// @version      0.5
// @description  进入课程自动注入,对网络要求较高.
// @match        https://l.shou.org.cn/study/learnCatalogNew.aspx?*
// @license      GPL-3.0 License
// @run-at       document-end
// @namespace https://www.omitkit.com
// ==/UserScript==

(function () {
    $("#leftBox").append('<div id="tools" style="width: 200px;padding-bottom: 10px;background-color: white;position: fixed;right: 100px;top: 200px;border: #ddd 1px solid;"><div class="am-panel-hd" style="border-color: #ddd;padding: 0 0 0 10px;"><h3 id="title-console" class="am-panel-title"><span class="am-icon am-icon-pie-chart"></span>工具栏</h3></div><a id="changeColor" href="#" class="am-btn am-btn-success am-btn-default" style="margin: 10px 10px 0 10px;display: block;">快速看完<span class="am-icon am-icon-chevron-circle-right"></span></a><a id="waterVideo" href="#" class="am-btn am-btn-success am-btn-default" style="margin: 10px 10px 0 10px;display: block;">开启水视频(测试)<span class="am-icon am-icon-chevron-circle-right"></span></a></div></div>');
    $("#changeColor").click(function () {
        $("#title-console").text("正在观看,请等待...");
        $(".tip").each(function (i, item) {
            let id = $(item).prop("id");
            $.ajax({
                type: "POST",
                async: false,
                url: "https://l.shou.org.cn/study/action-directory-getCell",
                data: { "cellId": id }
            });
        });
        $("#title-console").text("处理完成，可刷新查看");
    });

    $("#waterVideo").click(function () {
        $("#leftBox").append('<div id="tools-videos" style="width: 300px;padding-bottom: 10px;background-color: white;position: absolute;left: 30px;top: 63px;border: #ddd 1px solid;"><div class="am-panel-hd videosUp" style="cursor: pointer;border-color: #ddd;padding: 0 0 0 10px;"><h3 id="title-console2" class="am-panel-title"><span class="am-icon am-icon-pie-chart"></span>必看视频列表</h3></div><div id="videoList"></div>');
        $("#title-console").text("开始筛选,请等待...");
        $(".sh-res-h").each(function (i, item) {
            let link = $(item).find('a').prop('href');
            let id = link.substring(link.indexOf('cellId=') + 7);
            let status = $(item).find("img").prop("title") !== "已完成" ? "red" : "#77b723;";
            $.ajax({
                type: "POST",
                url: "https://l.shou.org.cn/study/action-directory-getCell",
                data: { "cellId": id },
                success: function (result) {
                    if (result.isVideo && result.canView == "1") {
                        $("#videoList").append('<div id="div' + i + '"><a id="' + i + '" data-url="' + link + '" href="#" class="am-btn am-btn-success am-btn-default iframeOpen" style="margin: 10px 10px 0 10px;display:inline-block;overflow: auto;background:' + status + ';">' + $(item).find('a').prop('title') + '<span class="am-icon am-icon-chevron-circle-right"></span></a></div>');
                    }
                }
            });
        });
        $("#title-console").text("过滤完成,可点击挂载!");
        $("#leftBox").append('<div id="videoTools" style="width: 200px;padding-bottom: 10px;background-color: white;position: fixed;right: 100px;top: 400px;border: #ddd 1px solid;"><div class="am-panel-hd" style="border-color: #ddd;padding: 0 0 0 10px;"><h3 id="title-console" class="am-panel-title">水视频模块加载完成</h3></div><a id="unfoldAll" href="#" class="am-btn am-btn-success am-btn-default" style="margin: 10px 10px 0 10px;display: block;">展开未看<span class="am-icon am-icon-chevron-circle-right"></span></a><a id="fullVideoAll" href="#" class="am-btn am-btn-success am-btn-default" style="margin: 10px 10px 0 10px;display: block;">一键全屏<span class="am-icon am-icon-chevron-circle-right"></span></a><div style="padding: 0 5px;margin-top: 10px;color: #d35e0c;background: #9bdbee;"><p style="line-height: 20px;">Tips:视频加载速度与网速有关,最好在宿舍点击。教室网太差了！特别是人多且同时访问的时候，很容易403</p></div></div>');
    });

    $(document).on('click', ".iframeOpen", function () {
        let name = "if" + $(this).prop("id");
        $(this).text("点击刷新");
        if (document.getElementById(name)) {
            document.getElementById(name).contentWindow.location.reload();
        } else {
            $(this).parent().append("<a id='full-" + $(this).prop("id") + "' class='am-btn am-btn-success am-btn-default fullPlay'  style='margin: 10px 10px 0 10px;display: inline-block;overflow: auto;'>全屏播放</a><a class='am-btn am-btn-success am-btn-default closeIframe' style='margin: 10px 10px 0 10px;display: inline-block;overflow: auto;'>移除</a><iframe style='height: 300px;' id='" + name + "' src='" + $(this).data("url") + "'></iframe>");
        }
    });

    $(document).on('click', ".videosUp", function () {
        $("#videoList").slideToggle();
    });

    $(document).on('click', ".fullPlay", function () {
        let docu = $(this).parent().find("iframe")[0];
        docu.contentWindow.dp.fullScreen.request('web');
        docu.contentWindow.dp.video.play();
        docu.contentWindow.dp.volume(0, true, false);
    });
    $(document).on('click', ".closeIframe", function () {
        $(this).parent().remove();
    });

    $(document).on('click', "#unfoldAll", function () {
        $(".iframeOpen").each(function () {
            if (document.getElementById(this.id).style.background == "red") {
                document.getElementById(this.id).click();
            }
        });
    });

    $(document).on('click', "#fullVideoAll", function () {
        $(".fullPlay").each(function () {
            let id = this.id.substring(5);
            console.log(id);
            if (document.getElementById(this.id).style.background == "red") {
                document.getElementById(this.id).onclick();
            }
        });
    });
})();