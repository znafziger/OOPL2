import org.apache.http._
import org.apache.http.client.entity._
import org.apache.http.client.methods._
import org.apache.http.impl.client._
import org.apache.http.client.utils._
import org.apache.http.message._
import org.apache.http.params._
import java.net.URL
import pageSummary.PageSummary


object SearchEngine extends App{
	def fetch(URL: String):String = {
		val httpget = new HttpGet(s"${URL}") //(url + "?" + query)
		val responsebody = new DefaultHttpClient().execute(httpget, new BasicResponseHandler())
		responsebody
	}
	def getLinks( html : String , baseURL : String) : List[String] = {
		// See http://www.mkyong.com/regular-expressions/how-to-extract-html-links-with-regular-expression/ for explanation of regex
		val aTagRegex = """(?i)<a([^>]+)>(.+?)</a>""".r
		val urlRegex = """\s*(?i)href\s*=\s*(\"([^"]*\")|'[^']*'|([^'">\s]+))""".r
		
		val opts = for ( a <- aTagRegex findAllMatchIn html ) yield urlRegex.findFirstMatchIn(a.toString)
		
		val hrefs = opts collect { case Some(x) => x group 1 }
		
		// remove leading and trailing quotes, if any
		val cleaned = hrefs map { _.stripPrefix("\"").stripPrefix("\'").stripSuffix("\"").stripPrefix("\'") } filter { ! _.startsWith("javascript") }
		
		// Use Java's URL class to parse the URL
		//   and get the full URL string (including implied context)
		val contextURL = new java.net.URL(baseURL)
		
		def getURL(x: String) = {
          var result = ""
          try {
            result = new java.net.URL(contextURL, x).toString()
          }
          catch {
            case e: java.net.MalformedURLException => Unit
          }
          result
        }
        
        (cleaned map { getURL(_) } ).filter(_.length > 0).toList

	}

	def getTerms(html:String, func: String=>Boolean):List[String] = {
		val terms = html.split("[^a-zA-Z0-9]")
		//not sure why, but empty strings were being added
		val clean = terms.toList.filter(x => x!= "")
		clean.filter(func)
	}

	//TODO:
	//Revise your SearchEngine.crawlAndIndex method: 
	//	def crawlAndIndex(startUrl: String, maxPages: Int, mode: String = 
		//"read", weight: Boolean = true): 
	//IndexedPages startUrl and maxPages serve the same purpose as in the
	// previous project.mode should be "read" or   "augment": for "read", 
	//no mixins are needed; for "augment", 
	//Augmentable should be mixed in to the returned object [4 pts].

	def crawlAndIndex(startURL:String, numPages:Int):List[PageSummary] = {
		var numCrawled = 0
		var URLS = List(startURL) //list of URLS to crawl
		var exploring = List(startURL) //list of URLS alread crawled or yet to crawl
		var summaries = List[PageSummary]() // list of page summaries to return
		while(numCrawled < numPages && URLS.size > 0){
			val URLToCrawl = URLS.last
			URLS = URLS.init
			//make a new pagesummary for the current page
			val newPS = new PageSummary(URLToCrawl, getTerms(fetch(URLToCrawl), {x=>x.length > 1}))
			var links = getLinks(fetch(URLToCrawl), URLToCrawl)
			for(link<-links){
				if(!exploring.contains(link)){
					exploring = link :: exploring
					URLS = link :: URLS
				}
			}
			summaries = newPS :: summaries
			numCrawled += 1

		}
		return summaries
	}
	
	def printBest(query : List[String], pages : List[PageSummary]) = {
		val scores = for(x <- pages) yield (x.url, x.fracMatching(query))
		for (x <- scores.sortBy(_._2).takeRight(5).reverse) println(x._1 + ": " + x._2.toString)
	}


	//TEST CODE HERE
	println("teat")

}



		