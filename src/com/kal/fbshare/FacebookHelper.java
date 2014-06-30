package com.kal.fbshare;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

public class FacebookHelper
{
	private Activity mActivity;

	public static String LOGIN_SUCCESS = "login_sucess";
	public static String LOGOUT_SUCCESS = "logout_sucess";
	public static String FRIENDLIST_SUCCESS = "friendlist_sucess";

	public static String LOGIN_ABORT_ERROR = "login_abort_error";
	public static String FRIENDLIST_ABORT_ERROR = "friendlist_abort_error";

	public static final String POST_SUCCESS = "post_sucess";
	public static final String POST_FAIL = "post_fail";
	public static final String POST_LOGIN_NOTFOUND = "post_login_notfound";

	public final String PROMOTING_MSG = "Hi...";

	private Bundle savedInstanceState;

	private String message = "";

	private String name = "";

	private String caption = "";

	private String link = "";

	private String pictureLink = "";

	private byte[] pictureArray = null;

	private String description = "";

	private Bitmap image;

	private GraphUser user;

	// private static final String MESSAGE_SUCCESS =
	// "Successfully posted on wall.";

	private ProgressDialog progressDialog;

	private boolean isPromotingMsg;

	private void dismissProgressDialog()
	{
		try
		{
			progressDialog.dismiss();
		} catch (Exception e)
		{
			// TODO: handle exception
		}
	}

	private boolean isInitLoginCall = false;

	public interface FacebookHelperListerner
	{
		// void onSessionStatusCall(Session session, SessionState state,
		// Exception exception);

		void onComplete(String result, int countMerge);

		// void onError(String state, int countMerge);
	}

	private FacebookHelperListerner facebookHelperListerner;

	public void setFBHelperListerner(FacebookHelperListerner facebookHelperListerner)
	{
		this.facebookHelperListerner = facebookHelperListerner;
	}

	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions", "publish_stream", "email", "friends_about_me", "friends_notes", "friends_birthday");
	// private static final List<String> PERMISSIONS = Arrays.asList("publish_actions", "publish_stream", "read_stream", "email", "user_about_me", "friends_about_me",
	// "friends_notes", "friends_birthday", "user_games_activity");

	private final String PENDING_ACTION_BUNDLE_KEY = "com.facebook.samples.hellofacebook:PendingAction";

	private Session.StatusCallback statusCallback = new SessionStatusCallback();

	private enum PendingAction
	{
		NONE, POST_PHOTO, POST_STATUS_UPDATE, POST_STATUS_PHOTO, POST_LINK
	}

	private PendingAction pendingAction = PendingAction.NONE;

	public FacebookHelper(Activity activity)
	{
		mActivity = activity;
		initHelper();
	}

	public FacebookHelper(Activity activity, Bundle savedInstanceState)
	{
		mActivity = activity;
		this.savedInstanceState = savedInstanceState;
		initHelper();
	}

	private void initHelper()
	{
		progressDialog = new ProgressDialog(mActivity);
		progressDialog.setTitle("Facebook");
		progressDialog.setMessage("Fetching...");
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.setCancelable(false);
		progressDialog.setOnCancelListener(new OnCancelListener()
		{

			@Override
			public void onCancel(DialogInterface dialog)
			{
				// TODO Auto-generated method stub
				facebookHelperListerner.onComplete(FRIENDLIST_ABORT_ERROR, 0);
				isAborted = true;
			}
		});
		progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
				facebookHelperListerner.onComplete(FRIENDLIST_ABORT_ERROR, 0);
				isAborted = true;
			}
		});

		Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

		Session session = Session.getActiveSession();
		if (session == null)
		{
			if (savedInstanceState != null)
			{
				session = Session.restoreSession(mActivity, null, statusCallback, savedInstanceState);
				String name = savedInstanceState.getString(PENDING_ACTION_BUNDLE_KEY);
				pendingAction = PendingAction.valueOf(name);
			}
			if (session == null)
			{
				session = new Session(mActivity);
			}
			Session.setActiveSession(session);
			if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED))
			{
				isInitLoginCall = true;
				login(false);
			}
		}
	}

	private Session getActiveSession()
	{
		Session session = Session.getActiveSession();
		if (session == null)
		{
			session = new Session(mActivity);
		}
		Session.setActiveSession(session);
		return session;
	}

	private class SessionStatusCallback implements Session.StatusCallback
	{
		@Override
		public void call(Session session, SessionState state, Exception exception)
		{
			onSessionStateChange(session, state, exception);
			// fetchUserInfo();
			Log.i("log_tag", "call state: " + state);

			if (state == SessionState.OPENED)
			{
				// facebookHelperListerner.onSessionStatusCall(session, state,
				// exception);
				if (isInitLoginCall)
				{
					isInitLoginCall = false;
				}
				else
				{
					if (isPromotingMsg)
					{
						postStatusUpdate(PROMOTING_MSG);
					}
					else
					{
						facebookHelperListerner.onComplete(LOGIN_SUCCESS, 0);
					}

				}

			}
			if (state == SessionState.CLOSED)
			{
				facebookHelperListerner.onComplete(LOGOUT_SUCCESS, 0);
			}

			if (state == SessionState.CLOSED_LOGIN_FAILED)
			{
				logout();
				// this.isPromotingMsg = isPromotingMsg;

				facebookHelperListerner.onComplete(LOGIN_ABORT_ERROR, 0);
			}

		}
	}

	private void onSessionStateChange(Session session, SessionState state, Exception exception)
	{
		if (pendingAction != PendingAction.NONE && (exception instanceof FacebookOperationCanceledException || exception instanceof FacebookAuthorizationException))
		{
			new AlertDialog.Builder(mActivity).setTitle("Canceled").setMessage("Unable to perform selected action because permissions were not granted.")
					.setPositiveButton("Ok", null).show();
			pendingAction = PendingAction.NONE;
		}
		else if (state == SessionState.OPENED_TOKEN_UPDATED)
		{
			handlePendingAction();
		}
	}

	public boolean appInstalledOrNot()
	{
		// PackageManager localPackageManager = mActivity.getPackageManager();
		// try
		// {
		// localPackageManager.getPackageInfo("com.facebook.katana", 1);
		// return true;
		// } catch (PackageManager.NameNotFoundException
		// localNameNotFoundException)
		// {
		// return false;
		// }

		return false;
	}

	public boolean isLogin()
	{
		Session session = getActiveSession();
		if (!session.isOpened() && !session.isClosed())
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	public void login(boolean isPromotingMsg)
	{
		this.isPromotingMsg = isPromotingMsg;
		Session session = getActiveSession();
		// SessionState state = session.getState();
		// state = SessionState.CREATED;
		if (!session.isOpened() && !session.isClosed())
		{
			Log.e("log_tag", "openForRead");

			Session.OpenRequest openRequest = new Session.OpenRequest(mActivity);
			openRequest.setCallback(statusCallback);
			openRequest.setDefaultAudience(SessionDefaultAudience.FRIENDS);
			// if(appInstalledOrNot())
			// {
			// openRequest.setLoginBehavior(SessionLoginBehavior.SSO_WITH_FALLBACK);
			// }
			// else
			// {
			openRequest.setLoginBehavior(SessionLoginBehavior.SUPPRESS_SSO);
			// }

			openRequest.setPermissions(PERMISSIONS);

			session.openForPublish(openRequest);
		}
		else
		{
			Log.e("log_tag", "openActiveSession");
			Session.openActiveSession(mActivity, true, statusCallback);
		}
	}

	public void logout()
	{
		Session session = getActiveSession();
		if (session != null)
		{
			session.closeAndClearTokenInformation();
			Session.setActiveSession(null);
		}

		// if (!session.isClosed())
		// {
		// session.closeAndClearTokenInformation();
		// }
	}

	public void onStart()
	{
		try
		{
			getActiveSession().addCallback(statusCallback);
		} catch (Exception e)
		{
			// TODO: handle exception

		}

	}

	public void onStop()
	{
		try
		{
			getActiveSession().removeCallback(statusCallback);
		} catch (Exception e)
		{
			// TODO: handle exception

		}

	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.e("log_tag", "onActivityResult: ");
		getActiveSession().onActivityResult(mActivity, requestCode, resultCode, data);
	}

	public void onSavedInstanceState(Bundle outState)
	{
		Session session = getActiveSession();
		Session.saveSession(session, outState);
		outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name());
	}

	public void shareMessageOnFriendWall(Bundle params)
	{

		WebDialog feedDialog = (new WebDialog.FeedDialogBuilder(mActivity, Session.getActiveSession(), params)).setOnCompleteListener(new OnCompleteListener()
		{

			@Override
			public void onComplete(Bundle values, FacebookException error)
			{

				if (error != null)
				{
					if (error instanceof FacebookOperationCanceledException)
					{
						Toast.makeText(mActivity.getApplicationContext(), "Request cancelled", Toast.LENGTH_SHORT).show();
					}
					else
					{
						Toast.makeText(mActivity.getApplicationContext(), "Network Error", Toast.LENGTH_SHORT).show();
					}
				}
				else
				{
					final String requestId = values.getString("post_id");
					if (requestId != null)
					{
						facebookHelperListerner.onComplete(POST_SUCCESS, 0);
					}
					else
					{
						facebookHelperListerner.onComplete(POST_FAIL, 0);
					}
				}

			}

		}).build();
		feedDialog.show();

	}

	public void sendRequestDialog()
	{
		Bundle params = new Bundle();
		params.putString("message", "We need to talk, please message me (Sent from Spydy Contacts)?");
		params.putString("to", "100003522159987");

		WebDialog requestsDialog = (new WebDialog.RequestsDialogBuilder(mActivity, Session.getActiveSession(), params)).setOnCompleteListener(new OnCompleteListener()
		{

			@Override
			public void onComplete(Bundle values, FacebookException error)
			{
				if (error != null)
				{
					if (error instanceof FacebookOperationCanceledException)
					{
						Toast.makeText(mActivity.getApplicationContext(), "Request cancelled", Toast.LENGTH_SHORT).show();
					}
					else
					{
						Toast.makeText(mActivity.getApplicationContext(), "Network Error", Toast.LENGTH_SHORT).show();
					}
				}
				else
				{
					final String requestId = values.getString("request");
					if (requestId != null)
					{
						Toast.makeText(mActivity.getApplicationContext(), "Request sent", Toast.LENGTH_SHORT).show();
					}
					else
					{
						Toast.makeText(mActivity.getApplicationContext(), "Request cancelled", Toast.LENGTH_SHORT).show();
					}
				}
			}

		}).build();
		requestsDialog.show();

		// WebDialog requestsDialog = (
		// new WebDialog.Builder(mActivity,
		// Session.getActiveSession(),"send",
		// params))
		// .setOnCompleteListener(new OnCompleteListener() {
		//
		// @Override
		// public void onComplete(Bundle values,
		// FacebookException error) {
		// if (error != null) {
		// if (error instanceof FacebookOperationCanceledException) {
		// Toast.makeText(mActivity.getApplicationContext(),
		// "Request cancelled",
		// Toast.LENGTH_SHORT).show();
		// } else {
		// Toast.makeText(mActivity.getApplicationContext(),
		// "Network Error",
		// Toast.LENGTH_SHORT).show();
		// }
		// } else {
		// final String requestId = values.getString("request");
		// if (requestId != null) {
		// Toast.makeText(mActivity.getApplicationContext(),
		// "Request sent",
		// Toast.LENGTH_SHORT).show();
		// } else {
		// Toast.makeText(mActivity.getApplicationContext(),
		// "Request cancelled",
		// Toast.LENGTH_SHORT).show();
		// }
		// }
		// }
		//
		// })
		// .build();
		// requestsDialog.show();
	}

	@SuppressWarnings("unused")
	private boolean isAborted = false;

	public void getFriendList()
	{
		// Request.executeMyFriendsRequestAsync(Session.getActiveSession(), new
		// Request.GraphUserListCallback()
		// {
		//
		// @Override
		// public void onCompleted(List<GraphUser> users, Response response)
		// {
		// Log.d("Friends Length", "" + users.size());
		// for (int i = 0; i < users.size(); i++)
		// {
		// GraphUser graphUser = users.get(i);
		// Log.e("log_tag", "jason: " + graphUser.getInnerJSONObject());
		// }
		//
		// }
		// });

		// Request friendRequest =
		// Request.newMyFriendsRequest(getActiveSession(), new
		// Request.GraphUserListCallback()
		// {
		// @SuppressWarnings("unchecked")
		// @Override
		// public void onCompleted(List<GraphUser> users, Response response)
		// {
		//
		// dismissProgressDialog();
		// if (isAborted)
		// {
		// return;
		// }
		// if (response.getError() == null)
		// {
		//
		// Log.e("log_tag", "response: " + response.toString());
		//
		// // FacebookContactsTask contactsTask = new
		// FacebookContactsTask(mActivity)
		// // {
		// // @Override
		// // protected void onPostExecute(String result)
		// // {
		// // // TODO Auto-generated method stub
		// // super.onPostExecute(result);
		// //
		// // facebookHelperListerner.onComplete(FRIENDLIST_SUCESS, countMerge);
		// //
		// // }
		// //
		// // @Override
		// // protected void onCancelled()
		// // {
		// // super.onCancelled();
		// // facebookHelperListerner.onComplete(FRIENDLIST_ABORT_ERROR,
		// countMerge);
		// // };
		// // };
		// // contactsTask.execute(users);
		// }
		// else
		// {
		// facebookHelperListerner.onComplete(null, 0);
		// }
		//
		// // Log.e("log_tag", "Jason: " + users.toString());
		//
		// }
		//
		// });
		// Bundle params = new Bundle();
		// params.putString("fields",
		// "id,name, birthday, email, picture.height(600).width(600)");
		// isAborted = false;
		// friendRequest.setParameters(params);
		//
		// dismissProgressDialog();
		// progressDialog.setMessage("Fetching...");
		// progressDialog.show();
		//
		// friendRequest.executeAsync();

		String fqlQuery = "SELECT uid, name, pic_square, contact_email FROM user WHERE uid IN " + "(SELECT uid2 FROM friend WHERE uid1 = me())";
		Bundle params1 = new Bundle();
		params1.putString("q", fqlQuery);
		Session session = Session.getActiveSession();
		Request request = new Request(session, "/fql", params1, HttpMethod.GET, new Request.Callback()
		{
			public void onCompleted(Response response)
			{
				// Log.e("log_tag", "Friend List response: " + response);
				// Log.e("log_tag", "Friend List jason: " +
				// response.getGraphObject().getInnerJSONObject());

				String result = "" + response.getGraphObject().getInnerJSONObject();
				facebookHelperListerner.onComplete(result, 123456);

				// FacebookContactsTask contactsTask = new
				// FacebookContactsTask(mActivity)
				// {
				// @Override
				// protected void onPostExecute(String result)
				// {
				// // TODO Auto-generated method stub
				// super.onPostExecute(result);
				//
				// mListener.onComplete(result, null);
				//
				// }
				// };
				// contactsTask.execute(response);
			}
		});
		Request.executeBatchAsync(request);
	}

	public void postStatusUpdate(String msg)
	{
		message = msg;
		pendingAction = PendingAction.POST_STATUS_UPDATE;

		progressDialog.setMessage("Please wait...");
		progressDialog.show();
		performPublish();
	}

	private void postStatusUpdate()
	{
		Log.e("log_tag", "postStatusUpdate: " + message);
		if (user != null && hasPublishPermission())
		{
			Request request = Request.newStatusUpdateRequest(getActiveSession(), message, new Request.Callback()
			{
				@Override
				public void onCompleted(Response response)
				{
					Log.e("log_tag", "Error: " + response.getError());
					dismissProgressDialog();
					if (isPromotingMsg)
					{
						facebookHelperListerner.onComplete(LOGIN_SUCCESS, 0);
						return;
					}

					FacebookRequestError frError = response.getError();
					if (frError != null)
					{
						// showToast(frError.getErrorMessage().substring(frError.getErrorMessage().lastIndexOf(")")
						// + 1));
					}
					else
					{
						// facebookHelperListerner.onComplete(POST_SUCESS, 0);

					}
					// logout();
					Log.d("log_tag", "GraphObject: " + response.getGraphObject());
				}
			});
			request.executeAsync();
		}
		else
		{
			pendingAction = PendingAction.POST_STATUS_UPDATE;
		}
	}

	public void postLink(String mMessage, String mName, String mCaption, String mDescription, String mLink, String picLink)
	{

		message = mMessage;
		name = mName;
		caption = mCaption;
		description = mDescription;
		link = mLink;
		pictureLink = picLink;
		Log.e("log_tag", "Posting link..");
		pendingAction = PendingAction.POST_LINK;
		performPublish();
	}

	private void postLinkWithImage()
	{

		if (user != null && hasPublishPermission())
		{

			Bundle postParams = new Bundle();
			postParams.putString("message", message);
			postParams.putString("name", name);
			postParams.putString("caption", caption);
			postParams.putString("description", description);
			postParams.putString("link", link);
			postParams.putString("picture", pictureLink);

			Request.Callback callback = new Request.Callback()
			{
				public void onCompleted(Response response)
				{

					// JSONObject graphResponse =
					// response.getGraphObject().getInnerJSONObject();
					// String postId = null;
					// try
					// {
					// postId = graphResponse.getString("id");
					// } catch (JSONException e)
					// {
					// Log.i("log_tag", "JSON error " + e.getMessage());
					// }

					// FacebookRequestError error = response.getError();
					// if (error != null)
					// {
					// Toast.makeText(mActivity.getApplicationContext(),
					// MESSAGE_FAIL + " " + error.getErrorMessage(),
					// Toast.LENGTH_SHORT).show();
					// }
					// else
					// {
					// Toast.makeText(mActivity.getApplicationContext(),
					// MESSAGE_SUCCESS, Toast.LENGTH_LONG).show();
					// }

					// logout();
				}
			};

			Request request = new Request(getActiveSession(), "100002152179898/feed", postParams, HttpMethod.POST, callback);

			RequestAsyncTask task = new RequestAsyncTask(request);
			task.execute();
		}
		else
		{
			pendingAction = PendingAction.POST_LINK;
		}
	}

	public void postPictureAndStatus(String msg, String mCaption, byte[] picArray)
	{
		Log.e("log_tag", "Posting picture and status");
		message = msg;
		caption = mCaption;
		pictureArray = picArray;

		try
		{
			progressDialog.dismiss();
		} catch (Exception e)
		{

		}
		progressDialog = ProgressDialog.show(mActivity, "", "Posting photo..");
		progressDialog.setCanceledOnTouchOutside(false);

		pendingAction = PendingAction.POST_STATUS_PHOTO;
		performPublish();
	}

	private void postPictureAndStatus()
	{

		if (user != null && hasPublishPermission())
		{

			// Bitmap bitmap =
			// BitmapFactory.decodeResource(mActivity.getResources(),
			// R.drawable.ic_launcher);
			// ByteArrayOutputStream baos = new ByteArrayOutputStream();
			// bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			// byte[] byteArrayImage = baos.toByteArray();

			Bundle postParams = new Bundle();
			postParams.putString("caption", caption);
			postParams.putByteArray("picture", pictureArray);
			postParams.putString("message", message);

			Request.Callback callback = new Request.Callback()
			{
				public void onCompleted(Response response)
				{
					try
					{
						progressDialog.dismiss();
					} catch (Exception e)
					{

					}
					facebookHelperListerner.onComplete(POST_SUCCESS, 0);
					// JSONObject graphResponse =
					// response.getGraphObject().getInnerJSONObject();
					// String postId = null;
					// try
					// {
					// postId = graphResponse.getString("id");
					// } catch (JSONException e)
					// {
					// Log.i("log_tag", "JSON error " + e.getMessage());
					// }
					// FacebookRequestError error = response.getError();
					// if (error != null)
					// {
					// Toast.makeText(mActivity.getApplicationContext(),
					// error.getErrorMessage(), Toast.LENGTH_SHORT).show();
					// }
					// else
					// {
					// // Toast.makeText(mActivity.getApplicationContext(),
					// postId, Toast.LENGTH_LONG).show();
					// }
				}
			};

			Request request = new Request(getActiveSession(), "me/photos", postParams, HttpMethod.POST, callback);

			RequestAsyncTask task = new RequestAsyncTask(request);
			task.execute();
		}
		else
		{
			pendingAction = PendingAction.POST_STATUS_PHOTO;
		}
	}

	protected void showToast(String string)
	{
		// TODO Auto-generated method stub
		Toast.makeText(mActivity, string, Toast.LENGTH_SHORT).show();
	}

	public void postPhoto(Bitmap bitmap)
	{
		this.image = bitmap;
		pendingAction = PendingAction.POST_PHOTO;
		performPublish();
	}

	private void postPhoto()
	{
		if (hasPublishPermission())
		{
			Request request = Request.newUploadPhotoRequest(getActiveSession(), image, new Request.Callback()
			{
				@Override
				public void onCompleted(Response response)
				{
					Log.e("log_tag", "Error: " + response.getError());
					Log.d("log_tag", "GraphObject: " + response.getGraphObject());

				}
			});
			request.executeAsync();
		}
		else
		{
			pendingAction = PendingAction.POST_PHOTO;
		}
	}

	private boolean hasPublishPermission()
	{
		Session session = getActiveSession();
		return session != null && session.getPermissions().contains("publish_actions");
	}

	private void performPublish()
	{
		Log.e("log_tag", "performPublish " + hasPublishPermission());
		Session session = getActiveSession();
		if (session != null)
		{

			// if (hasPublishPermission())
			// {
			// We can do the action right away.
			if (user == null)
			{

				Request request = Request.newMeRequest(session, new Request.GraphUserCallback()
				{
					@Override
					public void onCompleted(GraphUser me, Response response)
					{
						// if (currentSession ==
						// sessionTracker.getOpenSession()) {
						user = me;
						Log.e("log_tag", "GraphUser onCompleted");
						Log.e("log_tag", "response: " + response.toString());
						handlePendingAction();
						// }
						// if (response.getError() != null) {
						// Log.e("log_tag",
						// "Error: "+response.getError());
						// }
					}
				});
				Request.executeBatchAsync(request);

			}
			else
			{
				handlePendingAction();
			}

			// }
			// else
			// {
			// // We need to get new permissions, then complete the action when
			// // we get called back.
			// // session.requestNewPublishPermissions(new
			// Session.NewPermissionsRequest(mActivity, PERMISSIONS));
			// }
		}
		else
		{
			facebookHelperListerner.onComplete(POST_LOGIN_NOTFOUND, 0);
		}
	}

	@SuppressWarnings("incomplete-switch")
	private void handlePendingAction()
	{
		Log.e("log_tag", "handlePendingAction");
		PendingAction previouslyPendingAction = pendingAction;
		// These actions may re-set pendingAction if they are still pending, but
		// we assume they
		// will succeed.
		pendingAction = PendingAction.NONE;
		switch (previouslyPendingAction)
		{
		case POST_PHOTO:
			postPhoto();
			break;
		case POST_STATUS_UPDATE:
			postStatusUpdate();
			break;
		case POST_STATUS_PHOTO:
			postPictureAndStatus();
			break;
		case POST_LINK:
			postLinkWithImage();
			break;
		}
	}

}
