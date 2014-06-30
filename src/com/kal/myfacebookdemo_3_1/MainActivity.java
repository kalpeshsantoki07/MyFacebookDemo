package com.kal.myfacebookdemo_3_1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.Session;
import com.kal.fbshare.FacebookHelper;
import com.kal.fbshare.FacebookHelper.FacebookHelperListerner;

public class MainActivity extends Activity
{

	Button btnLogin;

	FacebookHelper fbHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		fbHelper = new FacebookHelper(MainActivity.this);
		fbHelper.setFBHelperListerner(new FacebookHelperListerner()
		{

			@Override
			public void onComplete(String result, int countMerge)
			{
				// TODO Auto-generated method stub
				if (result != null)
				{
					if (result.equals(FacebookHelper.LOGIN_ABORT_ERROR))
					{
						showToast("Login Canceled!");
					}
					else if (result.equals(FacebookHelper.LOGIN_SUCCESS))
					{
						showToast("Login Success!");
						afterLoginSucess();
					}
					else if (result.equals(FacebookHelper.LOGOUT_SUCCESS))
					{
						// showToast("Logout Success!");
					}

					else if (result.equals(FacebookHelper.POST_LOGIN_NOTFOUND))
					{
						showToast("Login not Found.");

					}
					else if (result.equals(FacebookHelper.POST_SUCCESS))
					{
						showToast("Post Success!");
					}
					else if (result.equals(FacebookHelper.POST_FAIL))
					{
						showToast("Post Fail!");
					}

				}
				else
				{
					showToast("Something wrong! Please try again.");
				}

			}
		});
		btnLogin = (Button) findViewById(R.id.btnLogin);
		btnLogin.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				// TODO Auto-generated method stub
				shareViaFacebook();
			}
		});

	}

	protected void showToast(String string)
	{
		// TODO Auto-generated method stub
		Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
	}

	protected void afterLoginSucess()
	{
		// TODO Auto-generated method stub
		fbHelper.postStatusUpdate("Hi Testing");
	}

	private void shareViaFacebook()
	{
		Session session = Session.getActiveSession();
		if (!session.isOpened())
		{
			fbHelper.login(false);
		}
		else
		{
			afterLoginSucess();
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();
		fbHelper.onStart();
	}

	@Override
	public void onStop()
	{
		super.onStop();
		fbHelper.onStop();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		fbHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		fbHelper.onSavedInstanceState(outState);
	}

}
