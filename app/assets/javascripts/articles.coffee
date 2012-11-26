jQuery ($) -> 
   $div = $('.container div') 
   articleListUrl = $div.data('list') 

   loadArticleDiv = -> 
      $.get articleListUrl, (articles) -> 
         $.each articles, (index, art) -> 
            url = art["json_url"]
            row  = $('<div class="row"/>')
            row.append $('<div class="two columns mobile-one"/>').append $('<img/>').attr('src','http://placehold.it/50x50&text=[img]')
            content = $('<div class="ten columns"/>')
            row.append content
            $div.append row
            $div.append $('<hr/>')
            loadArticleDetails(url, content)  
            $.each art["tweets"], (index, tweet) ->
              twrow = $('<div class="row"/>')
              twrow.append $('<div class="two columns mobile-one"/>').html("<img src="+tweet["profile_image"]+" alt="+tweet["user_name"]+"/>")
              twrow.append $('<div class="ten columns"/>').append $('<p/>').html("<a href="+tweet["profile_image"]+">"+tweet["user_name"]+"</a> - "+tweet["tweet"]+"<br/>"+tweet["created_at"])
              menu = $('<ul class="inline-list"/>')
              menu.append $('<li/>').append $('<a/>').attr('href','').text("Reply")
              menu.append $('<li/>').append $('<a/>').attr('href','').text("Retweet")
              twrow.append menu
              content.append twrow

   articleDetailsUrl = (url) -> 
      $table.data('details').replace '0', url 

   loadArticleDetails = (url, content) -> 
      loading = $('<strong/>').text("Loading article ...")
      content.prepend loading
      $.get url, (article) -> 
         loading.hide()
         content.prepend $('<p/>').text(article.domain+" - "+article.summary+" ...") 
         content.prepend $('<strong/>').append $('<a/>').attr('href',article.url).text(article.title) 

   loadArticleDiv() 
