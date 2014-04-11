/**
 * @author Jonathan
 */

package com.abewy.android.apps.klyph.messenger.facebook.request;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import com.abewy.android.apps.klyph.core.KlyphSession;
import com.facebook.Session;
import android.net.ParseException;

public class SendThreadMessageRequest
{
	private DefaultHttpClient	httpClient		= new DefaultHttpClient();
	private HttpContext			httpContext		= new BasicHttpContext();
	private CookieStore			cookieStore		= new BasicCookieStore();
	private HttpResponse		httpResponse	= null;
	private HttpPost			httpPost		= null;
	private String				f				= "";

	public SendThreadMessageRequest()
	{
		// TODO Auto-generated constructor stub
	}

	public String execute(String id, String message, boolean paramBoolean)
	{
		String strEntity = "body=" + message + "&tids=id." + id + /*"&fb_dtsg=" + AppBase.g() +*/ "&__user=" + KlyphSession.getSessionUserId() + "&__ajax__=true&__metablock__=3";
		
		
		httpClient.getParams().setParameter("http.protocol.cookie-policy", "best-match");
		//httpClient.getCookieSpecs().register("lenient", new b(this));

		//HttpClientParams.setCookiePolicy(this.httpClient.getParams(), "lenient");

		httpPost = new HttpPost("https://touch.facebook.com/messages/send/?refid=12&m_sess=" + Session.getActiveSession().getAccessToken());
		httpPost.setHeader("User-Agent", "mozilla/4.0 (mobilephone scp-3200/us/1.0) netfront/3.1 mmp/2.0");
		httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.setHeader("Cookie", f);

		httpResponse = null;

		String[] arrayOfString1;
		int i;
		StringEntity localStringEntity;

		/*if (!this.f.equals(""))
		{
			this.cookieStore.clear();
			arrayOfString1 = this.f.split(";");
			i = 0;
		}
		
		if (i >= arrayOfString1.length)
			this.httpContext.setAttribute("http.cookie-store", this.cookieStore);*/
		
		try
		{
			localStringEntity = new StringEntity(strEntity, "UTF-8");
			this.httpPost.setEntity(localStringEntity);
		}
		catch (UnsupportedEncodingException localUnsupportedEncodingException)
		{
			localUnsupportedEncodingException = localUnsupportedEncodingException;
			System.out.println("HTTPHelp : UnsupportedEncodingException : " + localUnsupportedEncodingException);
			localStringEntity = null;
		}
		
		try
		{
			this.httpResponse = this.httpClient.execute(this.httpPost, this.httpContext);
			if (this.httpResponse == null)
			{
				/*//return "";
				String[] arrayOfString2 = arrayOfString1[i].split("=");
				BasicClientCookie localBasicClientCookie = new BasicClientCookie(arrayOfString2[0], arrayOfString2[1]);
				localBasicClientCookie.setDomain("facebook.com");
				this.cookieStore.addCookie(localBasicClientCookie);*/
				//i++;
				
			}
		}
		catch (ClientProtocolException localClientProtocolException)
		{
			System.out.println("HTTPHelp : ClientProtocolException : " + localClientProtocolException.getMessage());
		}
		catch (IOException localIOException1)
		{
			System.out.println("HTTPHelp : IOException : " + localIOException1);

			HttpEntity localHttpEntity = this.httpResponse.getEntity();
			try
			{
				String str = EntityUtils.toString(localHttpEntity);
				return str;
			}
			catch (ParseException localParseException)
			{
				localParseException.printStackTrace();
				return "";
			}
			catch (IOException localIOException2)
			{
				localIOException2.printStackTrace();
			}
		}
		
		return "";
	}
}
