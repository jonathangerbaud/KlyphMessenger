package com.abewy.android.apps.klyph.messenger.app;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.abewy.android.apps.klyph.core.KlyphFlags;
import com.abewy.android.apps.klyph.messenger.R;
import com.abewy.util.ApplicationUtil;
import com.abewy.util.PhoneUtil;

public class AboutActivity extends TitledActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setTitle(R.string.about_activity_title);

		ImageView companyLogo = (ImageView) findViewById(R.id.company_logo);

		companyLogo.setClickable(true);
		companyLogo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v)
			{
				PhoneUtil.openURL(AboutActivity.this, getString(R.string.company_url));
			}
		});

		TextView version = (TextView) findViewById(R.id.version);
		TextView appName = (TextView) findViewById(R.id.app_name);

		appName.setText(KlyphFlags.IS_PRO_VERSION == true ? R.string.app_pro_large_name : R.string.app_large_name);
		version.setText(getString(R.string.about_version, ApplicationUtil.getAppVersion(this)));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return false;
	}

	@Override
	protected int getLayout()
	{
		return R.layout.activity_about;
	}
}
