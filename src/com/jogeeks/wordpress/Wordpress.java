package com.jogeeks.wordpress;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.params.ClientPNames;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.jogeeks.mobipress.R;
import com.jogeeks.wordpress.listeners.OnApiRequestListener;
import com.jogeeks.wordpress.listeners.OnCategoriesListener;
import com.jogeeks.wordpress.listeners.OnCommentSubmittedListener;
import com.jogeeks.wordpress.listeners.OnCommentsReceivedListener;
import com.jogeeks.wordpress.listeners.OnConnectionFailureListener;
import com.jogeeks.wordpress.listeners.OnCreatePostListener;
import com.jogeeks.wordpress.listeners.OnCustomFieldsListener;
import com.jogeeks.wordpress.listeners.OnLoginListener;
import com.jogeeks.wordpress.listeners.OnPostReceivedListener;
import com.jogeeks.wordpress.listeners.OnPostsReceivedListener;
import com.jogeeks.wordpress.listeners.OnRegisterListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class Wordpress implements OnLoginListener, OnRegisterListener {

	public static final int LOGIN_FAILED = -1;
	public static final int LOGIN_SUCCESS = 0;
	public static final int LOGIN_USER_NAME_ERROR = 1;
	public static final int LOGIN_PASSWORD_ERROR = 2;
	public static final int LOGIN_CHECK_PASSWORD_AND_OR_USERNAME = 3;
	public static final int BAD_NONCE = 4;

	public static final int REGISTRATION_FAILED = -1;
	public static final int REGISTRATION_SUCCESS = 0;
	public static final int REGISTRATION_INVALID_USER_NAME = 1;
	public static final int REGISTRATION_INVALID_DISPLAY_NAME = 2;
	public static final int REGISTRATION_USER_NAME_IN_USE = 3;
	public static final int REGISTRATION_INVALID_EMAIL = 4;
	public static final int REGISTRATION_EMAIL_IN_USE = 5;
	public static final int REGISTRATION_INVALID_PASSWORD = 6;

	// http://wordpress.org/plugins/json-api/other_notes/#3.2.-Content-modifying-arguments
	public static String BASE_URL = "";
	static String API = "";

	static final String NONCE_URL = "get_nonce";
	static final String DATE_INDEX_URL = "get_date_index";

	private Context context;

	private String username;
	private String password;

	private OnLoginListener loginListener;
	private OnRegisterListener registerListener;
	private OnConnectionFailureListener onConnectionFailureListener;
	private WordpressResponseHandler<WPPost> postHandler;
	private WordpressResponseHandler<WPComment> commentHandler;
	private WordpressResponseHandler<JSONObject> apiRequestHandler;

	private AsyncHttpClient httpClient = new AsyncHttpClient();

	/**
	 * <h1>WordPress constructor. After initializing this constructor, you can
	 * call several core methods: login(Bundle userData), register(Bundle
	 * userData)</h1>
	 * 
	 * @param context
	 *            Pass the application's context to get this initialized.
	 */
	public Wordpress(Context context, OnConnectionFailureListener listener) {
		this.context = context;
		API = context.getString(R.string.api);
		BASE_URL = context.getString(R.string.url) + "/" + API + "/";

		httpClient.getHttpClient().getParams()
				.setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
		postHandler = new WordpressResponseHandler<WPPost>();
		commentHandler = new WordpressResponseHandler<WPComment>();
		onConnectionFailureListener = listener;

		postHandler.setOnConnectionFailureListener(onConnectionFailureListener);
		commentHandler
				.setOnConnectionFailureListener(onConnectionFailureListener);

	}

	/**
	 * <h1>Set OnLoginListener to your instance before calling this function</h1>
	 * 
	 * @param userData
	 *            Pass a bundle with two String values that represent user
	 *            credentials "username", "password"
	 */
	public void login(Bundle userData, OnLoginListener listener) {
		username = userData.getString("username");
		password = userData.getString("password");
		new WPLogin(username, password, listener);
	}

	/**
	 * <h1>Set OnRegisterListener to your instance before calling this function</h1>
	 * 
	 * @param userData
	 *            Pass a bundle with four String values that represent user info
	 *            "username", "password", "email", "nickname"
	 */
	public void register(Bundle userData, OnRegisterListener listener) {
		new WPRegister(userData, listener);
	}

	public void finish(Context context) {
		httpClient.cancelRequests(context, true);
	}

	/**
	 * <h1>Adds a custom field (also called meta-data) to a specified post which
	 * could be of any post type</h1>
	 * 
	 * @param pid
	 *            integer Post ID
	 * @param meta
	 *            WPCustomeField object
	 * @param unique
	 *            When set to true, the custom field will not be added if the
	 *            given key already exists among custom fields of the specified
	 *            post.
	 */
	public void addPostMeta(int pid, WPCustomField meta, boolean unique,
			OnCustomFieldsListener listener) {
		postHandler.setOnCustomFieldsListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("post_id", Integer.toString(pid));
		reqParams.add("meta_key", meta.getName());
		reqParams.add("meta_value", meta.getValue());
		reqParams.add("unique", Boolean.toString(unique));

		httpClient.get(BASE_URL + WPCustomField.ADD_POST_META, reqParams,
				postHandler);
	}

	/**
	 * <h1>Adds a custom field (also called meta-data) to a specified post which
	 * could be of any post type</h1>
	 * 
	 * @param pid
	 *            integer Post ID
	 * @param meta
	 *            WPCustomeField object with key and the new value
	 * @param previousValue
	 *            The old value of the custom field you wish to change. This is
	 *            to differentiate between several fields with the same key. If
	 *            omitted, and there are multiple rows for this post and meta
	 *            key, all meta values will be updated.
	 */
	public void updatePostMeta(int pid, WPCustomField meta,
			String previousValue, OnCustomFieldsListener listener) {
		postHandler.setOnCustomFieldsListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("post_id", Integer.toString(pid));
		reqParams.add("meta_key", meta.getName());
		reqParams.add("meta_value", meta.getValue());
		reqParams.add("prev_value", previousValue);

		httpClient.get(BASE_URL + WPCustomField.UPDATE_POST_META, reqParams,
				postHandler);
	}

	/**
	 * <h1>Adds a custom field (also called meta-data) to a specified post which
	 * could be of any post type</h1>
	 * 
	 * @param pid
	 *            integer Post ID
	 * @param meta
	 *            WPCustomeField object with key and the new value
	 */
	public void updatePostMeta(int pid, WPCustomField meta,
			OnCustomFieldsListener listener) {
		postHandler.setOnCustomFieldsListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("post_id", Integer.toString(pid));
		reqParams.add("meta_key", meta.getName());
		reqParams.add("meta_value", meta.getValue());

		httpClient.get(BASE_URL + WPCustomField.UPDATE_POST_META, reqParams,
				postHandler);
	}

	/**
	 * <h1>Delete a custom field (also called meta-data) to a specified post
	 * which could be of any post type</h1>
	 * 
	 * @param pid
	 *            integer Post ID
	 * @param meta
	 *            WPCustomeField object with key and a specific value. value is
	 *            provided to differentiate between several fields with the same
	 *            key. If left blank, all fields with the given key will be
	 *            deleted.
	 */
	public void deletePostMeta(int pid, WPCustomField meta,
			OnCustomFieldsListener listener) {
		postHandler.setOnCustomFieldsListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("post_id", Integer.toString(pid));
		reqParams.add("meta_key", meta.getName());
		reqParams.add("meta_value", meta.getValue());

		httpClient.get(BASE_URL + WPCustomField.DELETE_POST_META, reqParams,
				postHandler);
	}

	/**
	 * <h1>Delete a custom field (also called meta-data) to a specified post
	 * which could be of any post type</h1>
	 * 
	 * @param pid
	 *            integer Post ID
	 * @param String
	 *            The key of the field you will delete.
	 */
	public void deletePostMeta(int pid, String key,
			OnCustomFieldsListener listener) {
		postHandler.setOnCustomFieldsListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("post_id", Integer.toString(pid));
		reqParams.add("meta_key", key);

		httpClient.get(BASE_URL + WPCustomField.DELETE_POST_META, reqParams,
				postHandler);
	}

	/**
	 * <h1>Returns an ArrayList with all custom fields of a particular post or
	 * page.</h1>
	 * 
	 * @param pid
	 *            integer Post ID whose custom fields will be retrieved.
	 * 
	 * @return ArrayList<WPCustomData>
	 * 
	 * @see getPostCustomKeys, getPostCustomValues
	 */
	public void getPostCustom(int pid, OnCustomFieldsListener listener) {
		postHandler.setOnCustomFieldsListener(listener);
		httpClient.get(BASE_URL + WPCustomField.GET_POST_CUSTOM, postHandler);
	}

	/**
	 * @param pid
	 *            integer Post ID whose custom fields will be retrieved.
	 * 
	 * @return Returns an ArrayList<String> containing the keys of all custom
	 *         fields of a particular post or page.
	 * 
	 * @see getPostCustom, getPostCustomValues
	 */
	public void getPostCustomKeys(int pid, OnCustomFieldsListener listener) {
		postHandler.setOnCustomFieldsListener(listener);
		httpClient.get(BASE_URL + WPCustomField.GET_POST_KEYS, postHandler);
	}

	/**
	 * <h1>This function is useful if you wish to access a custom field that is
	 * not unique, i.e. has more than 1 value associated with it.</h1>
	 * 
	 * @param key
	 *            The key whose values you want returned.
	 * @param pid
	 *            integer Post ID whose custom fields will be retrieved.
	 * 
	 * @return </h1>Returns an ArrayList<String> containing the values of all
	 *         custom fields of a particular post or page.</h2>
	 * 
	 * @see getPostCustomKeys, getPostCustom
	 */
	public void getPostCustomValues(String key, int pid,
			OnCustomFieldsListener listener) {
		postHandler.setOnCustomFieldsListener(listener);
		httpClient.get(BASE_URL + WPCustomField.GET_POST_KEYS, postHandler);
	}

	// TODO: most probably its best to use the Params, rather than supplying an
	// overloaded function for each case
	public void getPosts(OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);
		httpClient.get(BASE_URL + WPPost.POSTS_URL, postHandler);
	}

	public void getPosts(WPQuery query, OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);

		RequestParams reqParams = query.getQuery();
		httpClient.get(BASE_URL + WPPost.POSTS_URL, reqParams, postHandler);
	}

	public void getPosts(WPQuery query, int count, int page,
			OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);
		RequestParams reqParams = query.getQuery();

		httpClient.get(BASE_URL + WPPost.POSTS_URL + "?" + "count=" + count
				+ "&" + "page=" + page, reqParams, postHandler);
	}

	public void getPosts(int count, int page, OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);
		httpClient.get(BASE_URL + WPPost.POSTS_URL + "?" + "count=" + count
				+ "&" + "page=" + page, postHandler);
	}

	public void getRecentPosts(int count, int page,
			OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);
		httpClient.get(BASE_URL + WPPost.RECENT_POSTS_URL + "?" + "count="
				+ count + "&" + "page=" + page, postHandler);
	}

	public void getCustomPosts(int count, int page, String postType,
			OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);
		httpClient.get(BASE_URL + WPPost.POSTS_URL, postHandler);
	}

	public void getPostsByCategory(int cId, OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("id", Integer.toString(cId));
		httpClient.get(BASE_URL + WPPost.CATEGORY_POSTS_URL, reqParams,
				postHandler);
	}

	public void getPostsByCategory(int cId, int count, int page,
			OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("id", Integer.toString(cId));
		httpClient.get(BASE_URL + WPPost.CATEGORY_POSTS_URL + "?" + "count="
				+ count + "&" + "page=" + page, reqParams, postHandler);
	}

	public void getPostsByTag(int tId, OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("id", Integer.toString(tId));
		httpClient.get(BASE_URL + WPPost.TAG_POSTS_URL, reqParams, postHandler);
	}

	public void getPostsByTag(int tId, int count, int page,
			OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("id", Integer.toString(tId));
		httpClient.get(BASE_URL + WPPost.TAG_POSTS_URL + "?" + "count=" + count
				+ "&" + "page=" + page, reqParams, postHandler);
	}

	public void getPostsByAuthor(int aId, OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("id", Integer.toString(aId));
		httpClient.get(BASE_URL + WPPost.AUTHOR_POSTS_URL, reqParams,
				postHandler);
	}

	public void getPostsByAuthor(int aId, int count, int page,
			OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("id", Integer.toString(aId));
		httpClient.get(BASE_URL + WPPost.AUTHOR_POSTS_URL + "?" + "count="
				+ count + "&" + "page=" + page, reqParams, postHandler);
	}

	public void getPostsByDate(String date, OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("date", date);
		httpClient
				.get(BASE_URL + WPPost.DATE_POSTS_URL, reqParams, postHandler);
	}

	public void getPostsByDate(String date, int count, int page,
			OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("date", date);
		httpClient.get(BASE_URL + WPPost.DATE_POSTS_URL + "?" + "count="
				+ count + "&" + "page=" + page, reqParams, postHandler);
	}

	public void getPostsBySearch(String query, OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("search", query);
		httpClient.get(BASE_URL + WPPost.SEARCH_POSTS_URL, reqParams,
				postHandler);
	}

	public void getPostsBySearch(String query, int count, int page,
			OnPostsReceivedListener listener) {
		postHandler.setOnPostsReceivedListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("search", query);
		httpClient.get(BASE_URL + WPPost.SEARCH_POSTS_URL + "?" + "count="
				+ count + "&" + "page=" + page, reqParams, postHandler);
	}

	public void getPost(int pId, OnPostReceivedListener listener) {
		postHandler.setOnPostReceivedListener(listener);

		RequestParams reqParams = new RequestParams();
		reqParams.add("id", Integer.toString(pId));
		httpClient.get(BASE_URL + WPPost.POST_URL, reqParams, postHandler);
	}

	public void getPage(int pId,
			WordpressResponseHandler<WPPost> responseHandler) {
		RequestParams reqParams = new RequestParams();
		reqParams.add("id", Integer.toString(pId));
		httpClient.get(BASE_URL + WPPost.PAGE_URL, reqParams, responseHandler);
	}

	public void createPost(final WPPost post, final String attachment,
			OnCreatePostListener listener) {
		postHandler.setOnCreatePostListener(listener);

		httpClient.get(BASE_URL + NONCE_URL
				+ "/?controller=posts&method=create_post",
				new WordpressResponseHandler<WPPost>() {
					@Override
					public void onNonceRecieved(String nonce, String method) {
						this.setOnConnectionFailureListener(onConnectionFailureListener);

						String title, content, status;
						String categories = "";
						String tags = "";

						title = post.getTitle();
						content = post.getContent();
						status = post.getStatus();

						for (int i = 0; i < post.getCategories().size(); i++) {
							if (i == post.getCategories().size() - 1) {
								categories = categories
										+ post.getCategories().get(i).getSlug();
							} else {
								categories = categories
										+ post.getCategories().get(i).getSlug()
										+ ", ";
							}
						}

						for (int i = 0; i < post.getTags().size(); i++) {
							if (i == post.getTags().size() - 1) {
								tags = tags + post.getTags().get(i).getSlug();
							} else {
								tags = tags + post.getTags().get(i).getSlug()
										+ ", ";
							}
						}

						Log.d("cookie", new WPSession(context).getCookie());
						RequestParams reqParams = new RequestParams();
						reqParams.add("nonce", nonce);
						reqParams.add("cookie",
								new WPSession(context).getCookie());
						reqParams.add("title", title);
						reqParams.add("content", content);
						reqParams.add("status", status);
						reqParams.add("categories", categories);
						reqParams.add("tags", tags);

						if (!attachment.equals(null) || !attachment.equals("")) {
							File image = new File(attachment);
							Log.d("ATT2", image.getAbsolutePath());
							try {
								reqParams.put("attachment", image);
							} catch (FileNotFoundException e) {
							}
						}
						httpClient.post(BASE_URL + WPPost.CREATE_POST_URL,
								reqParams, postHandler);

						super.onNonceRecieved(nonce, method);
					}
				});
	}

	/**
	 * <h1>This function is useful if you wish to use a custom controller on
	 * your server</h1>
	 * 
	 * @param controller
	 *            The controller whose responsable of handling this request.
	 * @param method
	 *            The specific method whose going to process this request
	 * 
	 * @return </h1>Returns a JSONObject of response</h2>
	 * 
	 */
	public void apiRequest(String controller, String method,
			OnApiRequestListener listener) {
		apiRequestHandler = new WordpressResponseHandler<JSONObject>();
		apiRequestHandler.setOnApiRequestListener(listener);
		httpClient.get(BASE_URL + controller + "/" + method, apiRequestHandler);
	}

	/**
	 * <h1>This function is useful if you wish to use a custom controller on
	 * your server</h1>
	 * 
	 * @param controller
	 *            The controller whose responsable of handling this request.
	 * @param method
	 *            The specific method whose going to process this request
	 * @param params
	 *            if your request needs any request params, if it doesn't,
	 *            please user the overloaded method.
	 * @return </h1>Returns a JSONObject of response</h2>
	 * 
	 */
	public void apiRequest(String controller, String method,
			RequestParams params, OnApiRequestListener listener) {
		apiRequestHandler = new WordpressResponseHandler<JSONObject>();
		apiRequestHandler.setOnApiRequestListener(listener);
		httpClient.get(BASE_URL + controller + "/" + method, params,
				apiRequestHandler);
	}

	public void updatePost(final WPPost post, int userId, final String status,
			final WordpressResponseHandler<WPPost> responseHandler) {
		// TODO: add the cookie
		httpClient.get(BASE_URL + NONCE_URL
				+ "?controller=posts&method=update_post",
				new WordpressResponseHandler<WPPost>() {
					@Override
					public void onNonceRecieved(String nonce, String method) {
						RequestParams reqParams = new RequestParams();
						// reqParams.add("title", Integer.toString(pId));
						reqParams.add("title", post.getTitle());
						reqParams.add("content", post.getContent());
						reqParams.add("status", status);
						reqParams.add("nonce", nonce);

						httpClient.get(BASE_URL + WPPost.CREATE_POST_URL,
								reqParams, responseHandler);

						super.onNonceRecieved(nonce, method);
					}
				});
	}

	public void getCategoryIndex(OnCategoriesListener listener) {
		WordpressResponseHandler<WPCategory> responseHandler = new WordpressResponseHandler<WPCategory>();
		responseHandler.setOnCategoriesListener(listener);
		responseHandler
				.setOnConnectionFailureListener(onConnectionFailureListener);
		httpClient.get(BASE_URL + WPCategory.CATEGORY_INDEX, responseHandler);
	}

	public void getComments(int pId, OnCommentsReceivedListener listener) {
		commentHandler.setOnCommentsReceivedListener(listener);
		RequestParams reqParams = new RequestParams();
		reqParams.add("id", Integer.toString(pId));
		httpClient.get(BASE_URL + WPPost.POST_URL, reqParams, commentHandler);
	}

	public void submitComment(WPComment comment,
			OnCommentSubmittedListener listener) {
		commentHandler.setOnCommentsSubmittedListener(listener);
		RequestParams reqParams = new RequestParams();
		reqParams.add("name", comment.getName());
		reqParams.add("content", comment.getContent());
		reqParams.add("email", comment.getUrl());
		reqParams.add("post_id", Integer.toString(comment.getPostId()));
		httpClient.get(BASE_URL + WPComment.SUBMIT_COMMENT_URL, reqParams,
				commentHandler);
	}

	private class WPLogin {

		private String BAD_NONCE = "-1";

		private String nonceURL;
		private String cookieURL;

		private String userName;
		private String password;
		private RequestParams loginPar = new RequestParams();
		
		public WPLogin(String un, String pass, final OnLoginListener listener) {
			listener.OnLoginStart();

			/*
			 * 1 - OnLoginStart is called 2 - Nonce request. 1 - failure : calls
			 * OnLoginFailure 2 - success : make cookie request (actual login) 1
			 * - failure : calls OnLoginFailure 2 - success : calls
			 * OnLoginSuccess with a valid session
			 */

			userName = un;
			password = pass;

			cookieURL = BASE_URL.concat(context
					.getString(R.string.cookie_request));
			nonceURL = BASE_URL.concat(context.getString(R.string.loginNonce));

			final WPSession userSession = new WPSession(context);

			httpClient.get(nonceURL, new JsonHttpResponseHandler() {
				@Override
				public void onSuccess(int statusCode, Header[] headers,
						JSONObject response) {
					super.onSuccess(statusCode, headers, response);

					// Check nonce status
					if (isNonceOk(response)) {
						try {
							String nonce = response.getString("nonce");
							userSession.setNonce(nonce);
							
							loginPar.add("nonce", nonce);
							loginPar.add("username", userName);
							loginPar.add("password", password);

						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						httpClient.get(cookieURL, loginPar,
								new JsonHttpResponseHandler() {
									@Override
									public void onSuccess(int statusCode,
											Header[] headers,
											JSONObject response) {
										// Check cookie status
										if (isCookieOk(response)) {
											try {
												userSession
														.setSession(response);
											} catch (JSONException e) {
											}

											userSession
													.setStatus(Wordpress.LOGIN_SUCCESS);

											listener.OnLoginSuccess(userSession);

										} else {
											int code = Wordpress.LOGIN_FAILED;
											try {
												code = response.getInt("code");
											} catch (JSONException e) {
											}

											switch (code) {
											case 1:
												userSession
														.setStatus(Wordpress.LOGIN_USER_NAME_ERROR);
												listener.OnLoginFailure(userSession);
											case 2:
												userSession
														.setStatus(Wordpress.LOGIN_PASSWORD_ERROR);
												listener.OnLoginFailure(userSession);
											case 3:
												userSession
														.setStatus(Wordpress.LOGIN_CHECK_PASSWORD_AND_OR_USERNAME);
												listener.OnLoginFailure(userSession);
											}
										}
									}

									@Override
									public void onFailure(Throwable arg0,
											JSONObject arg1) {
										onConnectionFailureListener
												.OnConnectionFailed();
									}
								});
					} else {
						userSession.setStatus(Wordpress.BAD_NONCE);
						listener.OnLoginFailure(userSession);
					}
				}

				@Override
				public void onFailure(Throwable arg0, JSONObject arg1) {
					onConnectionFailureListener.OnConnectionFailed();
				}
			});
		}

		private boolean isNonceOk(JSONObject nr) {
			String status = null;

			try {
				status = nr.get("status").toString();
			} catch (Exception s) {

			}

			if (status.equals("ok")) {
				return true;
			} else {
				return false;
			}
		}

		private boolean isCookieOk(JSONObject nr) {
			String status = null;

			try {
				status = nr.get("status").toString();
			} catch (Exception s) {

			}

			if (status.equals("ok")) {
				return true;
			} else {
				return false;
			}
		}
	}

	private class WPRegister {

		private String nonceURL;
		private String registerURL;

		private String userName;
		private String displayName;
		private String email;
		private String password;

		RequestParams regPar = new RequestParams();

		private OnRegisterListener listener;

		public WPRegister(Bundle regData, OnRegisterListener listener) {
			this.listener = listener;

			userName = regData.getString("username");
			password = regData.getString("password");
			displayName = regData.getString("displayname");
			email = regData.getString("email");

			nonceURL = BASE_URL.concat(
					context.getString(R.string.registerNonce));
			registerURL = BASE_URL.concat(
					context.getString(R.string.register_request));

			register();

		}

		private void register() {
			listener.onRegisterStart();

			httpClient.get(nonceURL, new JsonHttpResponseHandler() {

				@Override
				public void onSuccess(int statusCode, Header[] headers,
						JSONObject response) {

					// Check nonce status
					if (isNonceOk(response)) {
						try {
							String nonce = response.getString("nonce");
							new WPSession(context).setNonce(nonce);
							// replace URL variables
							regPar.add("nonce", nonce);
							regPar.add("username", userName);
							regPar.add("display_name", displayName);
							regPar.add("email", email);
							regPar.add("password", password);

						} catch (JSONException e) {
							e.printStackTrace();
							listener.OnRegisterFailure(Wordpress.REGISTRATION_FAILED);
						}

						httpClient.get(registerURL, regPar,
								new JsonHttpResponseHandler() {

									@Override
									public void onSuccess(int statusCode,
											Header[] headers,
											JSONObject response) {
										int result = registrationStatus(response);

										if (result == Wordpress.REGISTRATION_SUCCESS) {
											listener.OnRegisterSuccess(result);
										} else {
											listener.OnRegisterFailure(result);
										}
									}

									@Override
									public void onFailure(Throwable arg0,
											JSONObject arg1) {
										onConnectionFailureListener
												.OnConnectionFailed();
									}
								});

					}else{
						//nonce is not ok
						listener.OnRegisterFailure(Wordpress.BAD_NONCE);
					}

				}

				@Override
				public void onFailure(Throwable arg0, JSONObject arg1) {
					onConnectionFailureListener.OnConnectionFailed();
				}
			});

		}

		private boolean isNonceOk(JSONObject nr) {
			String status = null;

			try {
				status = nr.get("status").toString();
			} catch (Exception s) {

			}

			if (status.equals("ok")) {
				return true;
			} else {
				return false;
			}
		}

		private int registrationStatus(JSONObject nr) {
			int code = -1;

			try {
				code = Integer.parseInt(nr.get("code").toString());
			} catch (Exception s) {

			}
			return code;
		}

	}

	public void setOnLoginListener(OnLoginListener l) {
		loginListener = l;
	}

	public void setOnRegisterListener(OnRegisterListener l) {
		registerListener = l;
	}

	@Override
	public void OnLoginSuccess(WPSession session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void OnLoginFailure(WPSession session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void OnRegisterSuccess(int error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void OnRegisterFailure(int error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRegisterStart() {
		// TODO Auto-generated method stub

	}

	@Override
	public void OnLoginStart() {
		// TODO Auto-generated method stub

	}

	protected static HashMap<String, String> parseResponseMeta(
			JSONObject response) throws JSONException {
		int count = 0, countTotal = 0, pages = 0;
		count = response.getInt("count");

		// TODO fix from server side
		// count_total is not included in getByCategory response and when the
		// response is only one page
		try {
			countTotal = response.getInt("count_total");
		} catch (JSONException e) {

		}
		pages = response.getInt("pages");

		HashMap<String, String> responseMeta = new HashMap<String, String>();
		responseMeta.put("count", Integer.toString(count));
		responseMeta.put("count_total", Integer.toString(countTotal));
		responseMeta.put("pages", Integer.toString(pages));

		return responseMeta;
	}
}
