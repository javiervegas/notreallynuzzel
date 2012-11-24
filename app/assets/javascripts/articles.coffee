jQuery ($) -> 
   $div = $('.container div') 
   articleListUrl = $div.data('list') 

   loadArticleDiv = -> 
      $.get articleListUrl, (articles) -> 
         $.each articles, (index, art) -> 
            url = art["json_url"]
            hidden  = $('<div class="hidden"/>').text(url) 
            panel  = $('<div class="panel"/>') 
            $div.append panel
            loadArticleDetails(hidden, panel)  
            $.each art["tweets"], (index, tweet) ->
              twrow = $('<div class="row"/>')
              twrow.append $('<div class="two columns"/>').html("<img src="+tweet["profile_image"]+" alt="+tweet["user_name"]+"/>")
              twrow.append $('<div class="ten columns"/>').html("<a href="+tweet["profile_image"]+">"+tweet["user_name"]+"</a> - "+tweet["tweet"]+"<br/>"+tweet["created_at"])
              panel.append twrow

   articleDetailsUrl = (url) -> 
      $table.data('details').replace '0', url 

   loadArticleDetails = (hidden, panel) -> 
      loading = $('<div class="row"/>').append $('<div class="twelve columns subheader"/>').text("Loading article ...")
      panel.prepend loading
      url = hidden.text()   
      $.get url, (article) -> 
         loading.hide()
         panel.prepend $('<div class="row"/>').append $('<div class="twelve columns summary"/>').text(article.domain+" - "+article.summary+" ...") 
         panel.prepend $('<div class="row"/>').append $('<div class="twelve columns subheader"/>').append $('<a/>').attr('href',article.url).text(article.title) 

   loadArticleDiv() 
