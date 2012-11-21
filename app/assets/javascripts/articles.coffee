jQuery ($) -> 
   $div = $('.container div') 
   articleListUrl = $div.data('list') 

   loadArticleDiv = -> 
      $.get articleListUrl, (articles) -> 
         $.each articles, (index, art) -> 
            url = art["json_url"]
            row  = $('<div class="panel"/>').text(url) 
            $div.append row 
            loadArticleDetails row  
            twrow = $('<div class="row"/>')
            twrow.append $('<div class="two columns"/>').html("<img src="+art["profile_image"]+" alt="+art["user_name"]+"/>")
            twrow.append $('<div class="ten columns"/>').html("<a href="+art["profile_image"]+">"+art["user_name"]+"</a> - "+art["tweet"]+"<br/>"+art["created_at"])
            row.append twrow

   articleDetailsUrl = (url) -> 
      $table.data('details').replace '0', url 

   loadArticleDetails = (divRow) -> 
      url = divRow.text()   
      $.get url, (article) -> 
         divRow.prepend $('<div class="row"/>').append $('<div class="twelve columns summary"/>').text(article.domain+" - "+article.summary+" ...") 
         divRow.prepend $('<div class="row"/>').append $('<div class="twelve columns subheader"/>').text(article.title) 

   loadArticleDiv() 
