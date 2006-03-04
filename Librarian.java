package plugins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import freenet.node.Node;
import freenet.pluginmanager.*;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.HighLevelSimpleClient;
import freenet.keys.FreenetURI;

public class Librarian implements FredPlugin, FredPluginHTTP {
	boolean goon = true;
	Random rnd = new Random();
	PluginRespirator pr;
	private static final String plugName = "Librarian";
	public void terminate() {
		goon = false;
	}
	
	private String getArrayElement(String[] array, int element) {
		try {
			return array[element];
		} catch (Exception e) {
			//e.printStackTrace();
			return "";
		}
	}
	public String handleHTTPPut(String path) throws PluginHTTPException {
		throw new PluginHTTPException();
	}
	public String handleHTTPPost(String path) throws PluginHTTPException {
		throw new PluginHTTPException();
	}
	
	private HashMap getElements(String path) {
		String[] getelements = getArrayElement(path.split("\\?"),1).split("\\&");
		HashMap ret = new HashMap();
		for (int i = 0; i < getelements.length ; i++) {
			int eqpos = getelements[i].indexOf("="); 
			if (eqpos < 1)
				// Unhandled so far
				continue;
			
			String key = getelements[i].substring(0, eqpos);
			String value = getelements[i].substring(eqpos + 1);

			ret.put(key, value);
			/*if (getelements[i].startsWith("page="))
				page = Integer.parseInt(getelements[i].substring("page=".length()));
				*/
		}
		return ret;
	}
	
	private void appendDefaultPageStart(StringBuffer out) {
		out.append("<HTML><HEAD><TITLE>" + plugName + "</TITLE></HEAD><BODY>\n");
		out.append("<CENTER><H1>" + plugName + "</H1><BR/><BR/><BR/>\n");
	}

	private void appendDefaultPageEnd(StringBuffer out) {
		out.append("</CENTER></BODY></HTML>");
	}
	
	private void appendDefaultPostFields(StringBuffer out) {
		appendDefaultPostFields(out, "", "");
	}
	
	private void appendDefaultPostFields(StringBuffer out, String search, String index) {
		out.append("Search for:<br/>");
		out.append("<form method=\"GET\"><input type=text value=\""+search
				+"\" name=\"search\" size=80/><br/><br/>");
		out.append("Using the index:<br/>");
		out.append("<input type=text name=\"index\" value=\"" + index + "\" size=80/>");
		out.append("<input type=submit value=\"Find!\"/></form>\n");
		// index - key to index
		// search - text to search for
	}
	
	
	private HashMap getFullIndex(String uri) throws Exception {
		HighLevelSimpleClient hlsc = pr.getHLSimpleClient();
		String index[] = new String(hlsc.fetch(new FreenetURI(uri)).asByteArray()).trim().split("\n");
		
		Vector uris = new Vector();
		HashMap keywords = new HashMap();
		
		int i;
		URIWrapper uriw = new URIWrapper();
		for (i = 0 ; i < index.length ; i++) {
			if (index[i].startsWith("!")) {
				/* Start new */
				uriw = new URIWrapper();
				uriw.URI = index[i].substring(1);
				uris.add(uriw);
			} else if (index[i].startsWith("+")) {
				/* Continue old */
				if (uriw.descr==null)
					uriw.descr = index[i].substring(1);
				else
					uriw.descr += "\n"+ index[i].substring(1);
			} else
				break;
			
		}
		
		for ( ; i < index.length ; i++) {
			if (!index[i].startsWith("?"))
				break;
			String parts[] = index[i].split(" ");
			HashSet keyuris = new HashSet();
			//System.err.println(":::" +  + ":::");
			for (int j = 1 ; j < parts.length ; j++) {
				keyuris.add(uris.get(Integer.parseInt(parts[j])));
				//System.err.println();
			}
			
			keywords.put(parts[0].substring(1), keyuris);
		}
		
		return keywords;
	}
	
	public String handleHTTPGet(String path) throws PluginHTTPException {
		StringBuffer out = new StringBuffer();
		String[] pathelements = path.split("\\?");
		/*
		String[] getelements = getArrayElement(pathelements, 1).split("\\?");
		for (int i = 0; i < getelements.length ; i++) {
			if (getelements[i].startsWith("page="))
				page = Integer.parseInt(getelements[i].substring("page=".length()));
		}
		*/
		HashMap getelem = getElements(path);
		//int page = (getelem.get("page") == null)?1:Integer.parseInt((String)getelem.get("page"));
		String indexuri = (String)getelem.get("index");
		String search = (String)getelem.get("search");
		indexuri = (indexuri==null)?"":indexuri;
		search = (search==null)?"":search;
		
		if (search.equals("")) {
			appendDefaultPageStart(out);
			appendDefaultPostFields(out);
			appendDefaultPageEnd(out);
			return out.toString();
		}
		
		try {
			/* Cache later! */
			//HighLevelSimpleClient hlsc = pr.getHLSimpleClient();
			//String index = new String(hlsc.fetch(new FreenetURI(indexuri)).asByteArray()).trim();
			/* /Cache!? */
			
			//imglist = imglist.replaceAll("\r","\n");
			
			/*do {
				i = imglist.hashCode();
				imglist = imglist.replaceAll("\n\n", "\n");
			} while (i != imglist.hashCode());*/
			
			//String[] imgarr = imglist.split("\n");
			
			
			HashMap index = getFullIndex(indexuri);
			
			appendDefaultPageStart(out);
			appendDefaultPostFields(out, search.replaceAll("\\+", " "), indexuri);

			out.append("Searching for: " + search.replaceAll("\\+", " ") + "\n");

			//String searchWords[] = search.replaceAll("%20", "+").split("+");
			// Get search result
			String searchWords[] = search.split("\\+");
			
			HashSet hs = new HashSet();
			{ // add all for the first word
				HashSet keyuris = (HashSet)index.get(searchWords[0].toLowerCase().trim());
				if (keyuris != null) {
					Iterator it = keyuris.iterator();
					while (it.hasNext())
						hs.add(it.next());
				}
			}
			for (int i = 0 ; i < searchWords.length ; i++) {
				HashSet keyuris = (HashSet)index.get(searchWords[i].toLowerCase().trim());
				Iterator it = hs.iterator();
				while (it.hasNext()) {
					Object o = it.next();
					if (!keyuris.contains(o))
						hs.remove(o);
				}
			}
			

			// Output results
			int results = 0;
			out.append("<table><tr>\n");
			Iterator it = hs.iterator();
			while (it.hasNext()) {
				URIWrapper o = (URIWrapper)it.next();
				out.append("<table border=1><tr><td align=center bgcolor=\"#D0D0D0\">\n");
				out.append("  <A HREF=\"/" + o.URI + "\">" + o.URI + "</A>\n");
				out.append("</td></tr><tr><td align=left>\n");
				out.append("<pre>" + o.descr + "</pre>\n");
				out.append("</td></tr></table>\n");
				results++;
			}
			out.append("</tr><table>\n");
			out.append("Found: " + results + " results\n");
			

			appendDefaultPageEnd(out);
			
			return out.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}
	}
	

	public void runPlugin(PluginRespirator pr) {
		this.pr = pr;
		
		//int i = (int)System.currentTimeMillis()%1000;
		while(goon) {
			/*
			FetchResult fr;
			try {
				fr = pr.getHLSimpleClient().fetch(new FreenetURI("freenet:CHK@j-v1zc0cuN3wlaCpxlKd6vT6c1jAnT9KiscVjfzLu54,q9FIlJSh8M1I1ymRBz~A0fsIcGkvUYZahZb5j7uepLA,AAEA--8"));
				System.err.println("  Got data from key, length = " + fr.size() + " Message: "
						+ new String(fr.asByteArray()).trim());
			} catch (Exception e) {
			}
			*/
			try {
				Thread.sleep(300000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
	}
	
	private class URIWrapper implements Comparable {
		public String URI;
		public String descr;
		
		public int compareTo(Object o) {
			if (o instanceof URIWrapper)
				return URI.compareTo(((URIWrapper)o).URI);
			return -1;
		}
	}

}
