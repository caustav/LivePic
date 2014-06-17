package com.relevance.photoextension.hotspot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.relevance.photoextension.R;
import com.relevance.photoextension.dao.MySQLiteHelper;
import com.relevance.photoextension.model.Hotspot;
import com.relevance.photoextension.model.Image;
import com.relevance.photoextension.utility.Constants;
import com.relevance.photoextension.utility.Utility;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class AddHotspotToImage extends Activity implements OnTouchListener,
		OnClickListener, OnLongClickListener {

	private final ArrayList<ImageView> hotspotIconForAdd = new ArrayList<ImageView>();
	private final ArrayList<Hotspot> hotSpots = new ArrayList<Hotspot>();
	private MySQLiteHelper dbHelper;
	private ViewGroup rootView;
	private View myView;
	private ImageView addButton;
	private ImageView selectedImg;
	private ImageView recordButton;
	private EditText textEdit;
	private EditText urlEdit;
	private int xDelta;
	private int yDelta;
	private int index;
	private static int counter = 0;
	private static int xDisp = 0;
	private static int yDisp = 0;
	private int mediaIdFlag;
	private static int imageLevelFlag;
	private String text;
	private String url;
	private String urlData;
	private String selectedMediaPath;
	private String imageFilePath = "";
	private String audioFilePath = "";
	private boolean canPlay = false;
	private boolean canRecord = true;
	private boolean isPlaying = false;
	private boolean isRecording = false;
	private boolean isEditable;
	private boolean isHotspotVisible = true;
	private Dialog dlg = null;
	private static MediaRecorder mediaRecorder = null;
	private static MediaPlayer mediaPlayer = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.edit_hotspot);

		imageFilePath = getIntent().getStringExtra("imageName");
		isEditable = getIntent().getBooleanExtra("edit", false);

		rootView = (ViewGroup) findViewById(R.id.root);
		selectedImg = (ImageView) findViewById(R.id.imageCenter);
		addButton = (ImageView) findViewById(R.id.add_hotspot_button);
		ImageView saveButton = (ImageView) findViewById(R.id.iv_save_hotspot);
		ImageView cameraButton = (ImageView) findViewById(R.id.iv_camera_hotspot);
		ImageView toggleButton = (ImageView) findViewById(R.id.iv_hide_hotspot);

		addButton.setOnClickListener(this);
		addButton.setOnLongClickListener(this);
		saveButton.setOnClickListener(this);
		cameraButton.setOnClickListener(this);
		toggleButton.setOnClickListener(this);

		dbHelper = new MySQLiteHelper(getApplicationContext());

		imageLevelFlag = 0;
		if (isEditable) {
			drawAvailableHotspot();
		}
	}

	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			Utility.setImage(AddHotspotToImage.this, imageFilePath, selectedImg);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == Constants.RQS_OPEN_AUDIO_MP3
				&& resultCode == RESULT_OK) {
			Uri selectedMediaUri = data.getData();
			selectedMediaPath = Utility.getRealPathFromURI(this,
					selectedMediaUri);
			recordButton.setImageResource(R.drawable.microphone_icon);
			audioFilePath = null;
			canRecord = true;
			canPlay = false;
			mediaIdFlag = 2;

		} else if (requestCode == Constants.CAMERA_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				Intent intent = getIntent();
				intent.putExtra("imageName", imageFilePath);
				intent.putExtra("edit", false);
				startActivity(intent);
				finish();

				Utility.mediaScanForCapturedImage(this, imageFilePath);
			} else {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.capture_cancelled),
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public boolean onLongClick(View v) {
		removeDialog(100);
		myView = selectedImg;
		if (imageLevelFlag == 0) {
			imageLevelFlag++;
			mediaIdFlag = 0;
			Hotspot h = new Hotspot(0, 0, "", "", null, 0);
			hotSpots.add(h);
			hotspotIconForAdd.add(selectedImg);
			showDialog(100);
		} else {
			showDialog(100);
		}
		return false;
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.add_hotspot_button:// to add hotspot to image
			addHotspot();
			break;

		case R.id.iv_save_hotspot:// to save the image with respective hotspots
			if (isEditable) {
				int pid = dbHelper.getImage(imageFilePath);
				dbHelper.deleteAllHotspot(pid);
				saveHotspotInDatabase();
				startHotspotDetailsActivity();
			} else {
				saveHotspotInDatabase();
			}
			finish();
			break;

		case R.id.iv_camera_hotspot: // to launch camera activity
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			imageFilePath = Utility.getCapturedImage();
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(imageFilePath)));
			startActivityForResult(intent, Constants.CAMERA_REQUEST_CODE);
			break;

		case R.id.iv_hide_hotspot: // to toggle hotspot hide/unhide
			toggleHotspots();
			break;

		}
	}

	private void saveHotspotInDatabase() {
		Image img = new Image(imageFilePath);
		dbHelper.addImage(img);
		for (Hotspot h : hotSpots) {
			dbHelper.addHotspot(h, img);
		}
		Toast.makeText(this, getResources().getString(R.string.hotspot_saved),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (!hotSpots.isEmpty()) {
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
						AddHotspotToImage.this);

				alertBuilder.setTitle(getResources()
						.getString(R.string.hotspot));
				alertBuilder.setCancelable(false);
				alertBuilder.setMessage(getResources().getString(
						R.string.hotspot_save));
				alertBuilder.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								saveHotspotInDatabase();
								if (isEditable) {
									startHotspotDetailsActivity();
								}
								finish();
							}
						});
				alertBuilder.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (isEditable) {
									startHotspotDetailsActivity();
								}
								finish();
							}
						});
				AlertDialog dlg = alertBuilder.create();
				dlg.show();
			} else {
				finish();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void toggleHotspots() {
		for (ImageView x : hotspotIconForAdd) {
			if (x.equals(selectedImg))
				continue;
			if (isHotspotVisible)
				x.setVisibility(View.INVISIBLE);
			else
				x.setVisibility(View.VISIBLE);
		}
		isHotspotVisible = !isHotspotVisible;
	}

	private void displacement() {
		if (counter < 40) {
			xDisp += 5;
			yDisp += 5;
			counter += 5;
		} else if (counter >= 40 && counter < 80) {
			xDisp += 5;
			yDisp -= 5;
			counter += 5;
		} else if (counter >= 80 && counter < 120) {
			xDisp -= 5;
			yDisp -= 5;
			counter += 5;
		} else if (counter >= 120 && counter < 160) {
			xDisp -= 5;
			yDisp += 5;
			counter += 5;
		} else {
			counter = 0;
			xDisp += 5;
			yDisp += 5;
		}
	}

	private void addHotspot() {
		ImageView hotspot = new ImageView(this);
		hotspot.setImageResource(R.drawable.boundary);
		hotspot.setClickable(true);
		hotspot.setEnabled(true);
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				48, 48);
		displacement();
		layoutParams.leftMargin = (AddHotspotToImage.this.getWindowManager()
				.getDefaultDisplay().getWidth()) / 2 - 25 + xDisp;
		layoutParams.topMargin = (AddHotspotToImage.this.getWindowManager()
				.getDefaultDisplay().getHeight()) / 2 - 25 + yDisp;
		hotspot.setLayoutParams(layoutParams);
		hotspot.setOnTouchListener(this);
		hotspot.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				myView = v;
				mediaIdFlag = 0;
				removeDialog(100);
				showDialog(100);
				return false;
			}
		});
		Hotspot hotspotProperty = new Hotspot(layoutParams.leftMargin,
				layoutParams.topMargin, "", "", null, 0);
		rootView.addView(hotspot);
		hotspotIconForAdd.add(hotspot);
		hotSpots.add(hotspotProperty);

		Log.d(Constants.ADD_HOTSPOT_LOGTAG,
				"Hotspot count after add button is clicked " + hotSpots.size());
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		final int X = (int) event.getRawX();
		final int Y = (int) event.getRawY();
		// find the index of the hotspot moved
		int index = -1, i = 0;
		for (ImageView x : hotspotIconForAdd) {
			if (x.equals(view)) {
				index = i;
			}
			++i;
		}
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) view
					.getLayoutParams();
			xDelta = X - lParams.leftMargin;
			yDelta = Y - lParams.topMargin;
			break;

		case MotionEvent.ACTION_UP:
			break;

		case MotionEvent.ACTION_POINTER_DOWN:
			break;

		case MotionEvent.ACTION_POINTER_UP:
			break;

		case MotionEvent.ACTION_MOVE:
			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view
					.getLayoutParams();
			layoutParams.leftMargin = X - xDelta;
			layoutParams.topMargin = Y - yDelta;
			view.setLayoutParams(layoutParams);
			hotSpots.get(index).setXPosition((int) view.getX());
			hotSpots.get(index).setYPosition((int) view.getY());
			break;
		}
		rootView.invalidate();
		return false;
	}

	@Override
	protected Dialog onCreateDialog(final int id) {

		index = hotspotIconForAdd.indexOf(myView);

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		if (id == 100) {
			reInitMedia();
			LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = li.inflate(R.layout.dailog_in_editmode, null);
			final TableLayout attach_rl = (TableLayout) v
					.findViewById(R.id.hotspot_attach);
			final RelativeLayout url_layout = (RelativeLayout) v
					.findViewById(R.id.hotspot_link_extended);
			url_layout.setVisibility(View.GONE);
			attach_rl.setVisibility(View.GONE);

			builder.setView(v);

			text = hotSpots.get(index).getInputText();
			url = hotSpots.get(index).getInputUrl();

			textEdit = (EditText) v.findViewById(R.id.round_text);
			urlEdit = (EditText) v.findViewById(R.id.et_round_text2);
			recordButton = (ImageView) v.findViewById(R.id.microphone);
			ImageView attachMedia = (ImageView) v.findViewById(R.id.audio);
			ImageView attachButton = (ImageView) v.findViewById(R.id.attach);
			ImageView linkButton = (ImageView) v.findViewById(R.id.link);
			ImageView saveButtonDlg = (ImageView) v
					.findViewById(R.id.saveButton);
			ImageView deleteButton = (ImageView) v
					.findViewById(R.id.deleteButton);
			ImageView backButton = (ImageView) v.findViewById(R.id.backButton);
			ImageView videoButton = (ImageView) v.findViewById(R.id.video);
			ImageView contactsButton = (ImageView) v
					.findViewById(R.id.contacts);
			ImageView locationButton = (ImageView) v
					.findViewById(R.id.location);
			ImageView shoppingButton = (ImageView) v
					.findViewById(R.id.shopping);

			if (isEditable) {
				mediaIdFlag = hotSpots.get(index).getMediaTypeFlag();
				if (mediaIdFlag == 2) {
					selectedMediaPath = hotSpots.get(index).getAudioFile();
				} else {
					audioFilePath = hotSpots.get(index).getAudioFile();
				}
				if (url != null && !url.isEmpty()) {
					url_layout.setVisibility(View.VISIBLE);
					urlEdit.setText(url);
				} else {
					url_layout.setVisibility(View.GONE);
				}
			}

			recordButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (selectedMediaPath != null
							&& !selectedMediaPath.isEmpty()) {

						AlertDialog.Builder alertDialogBuilder2 = new AlertDialog.Builder(
								AddHotspotToImage.this);
						alertDialogBuilder2
								.setMessage(
										getResources()
												.getString(
														R.string.overwrite_selected_media))
								.setCancelable(false)
								.setPositiveButton(android.R.string.yes,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int id) {

												selectedMediaPath = null;
												mediaIdFlag = 0;

												if (!isRecording && canRecord) {
													recordButton
															.setImageResource(R.drawable.stop);
													startRecordAudio();

												} else if (isRecording
														&& mediaRecorder != null) {
													stopRecordAudio();
													recordButton
															.setImageResource(R.drawable.play);
													Log.v(AUDIO_SERVICE,
															"recording stopped now click to play");
												} else if (!isPlaying
														&& canPlay) {
													recordButton
															.setImageResource(R.drawable.stop);
													startPlayingRecordedAudio();

												} else if (isPlaying
														&& mediaPlayer != null) {
													stopPlayingRecordedAudio();
													recordButton
															.setImageResource(R.drawable.play);
													Log.v(AUDIO_SERVICE,
															"recording stop played");
												}
											}
										})
								.setNegativeButton(android.R.string.no,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int id) {
												dialog.cancel();
											}
										});
						AlertDialog alertDialog2 = alertDialogBuilder2.create();
						alertDialog2.show();

					} else {
						if (!isRecording && canRecord) {
							recordButton.setImageResource(R.drawable.stop);
							startRecordAudio();

						} else if (isRecording && mediaRecorder != null) {
							stopRecordAudio();
							recordButton.setImageResource(R.drawable.play);
							Log.v(AUDIO_SERVICE,
									"Recording stopped.Click to play");

						} else if (!isPlaying && canPlay) {
							recordButton.setImageResource(R.drawable.stop);
							startPlayingRecordedAudio();

						} else if (isPlaying && mediaPlayer != null) {
							stopPlayingRecordedAudio();
							recordButton.setImageResource(R.drawable.play);
							Log.v(AUDIO_SERVICE, "recording stop played");
						}
						mediaIdFlag = 0;
					}
				}
			});

			if (hotSpots.get(index).getInputText() != null) {
				textEdit.setText(hotSpots.get(index).getInputText());
			}

			if (hotSpots.get(index).getAudioFile() != null
					&& hotSpots.get(index).getAudioFile() != "") {
				if (hotSpots.get(index).getMediaTypeFlag() == 2) {
					selectedMediaPath = hotSpots.get(index).getAudioFile();

				}
				if (hotSpots.get(index).getMediaTypeFlag() == 0) {
					audioFilePath = hotSpots.get(index).getAudioFile();
					recordButton.setImageResource(R.drawable.play);
					canPlay = true;
					isPlaying = false;
					canRecord = false;
				}
			}

			attachButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (attach_rl.getVisibility() != View.VISIBLE) {
						attach_rl.setVisibility(View.VISIBLE);
					} else {
						attach_rl.setVisibility(View.GONE);
					}
				}
			});

			linkButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (url_layout.getVisibility() != View.VISIBLE) {
						url_layout.setVisibility(View.VISIBLE);
						if (hotSpots.get(index).getInputUrl() != null)
							urlEdit.setText(hotSpots.get(index).getInputUrl());
					} else {
						urlData = urlEdit.getText().toString();
						if (urlData != null && !urlData.isEmpty()) {
							if (Utility.isValidUrl(urlData)) {
								hotSpots.get(index).setInputUrl(urlData);
								url_layout.setVisibility(View.GONE);
							} else {
								Toast.makeText(
										AddHotspotToImage.this,
										getResources().getString(
												R.string.invalid_url),
										Toast.LENGTH_LONG).show();
								urlEdit.setFocusable(true);
							}
						} else {
							url_layout.setVisibility(View.GONE);
						}
					}
				}
			});

			attachMedia.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (audioFilePath != null) {

						AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
								AddHotspotToImage.this);
						alertDialogBuilder.setTitle(getResources().getString(
								R.string.media_select));
						// set dialog message
						alertDialogBuilder
								.setMessage(
										getResources()
												.getString(
														R.string.overwite_recorded_audio))
								.setCancelable(false)
								.setPositiveButton(android.R.string.yes,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int id) {

												startActivityToChooseAudio();
											}
										})
								.setNegativeButton(android.R.string.no,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int id) {
												dialog.cancel();
											}
										});
						AlertDialog alertDialog = alertDialogBuilder.create();
						alertDialog.show();

					} else {
						startActivityToChooseAudio();
					}
				}
			});

			saveButtonDlg.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					saveTheChange();
				}
			});

			deleteButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {

					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
							AddHotspotToImage.this);
					alertDialogBuilder.setTitle(getResources().getString(
							R.string.hotspot));
					alertDialogBuilder
							.setMessage(
									getResources().getString(
											R.string.delete_options))
							.setCancelable(false)
							.setPositiveButton(
									getResources().getString(
											R.string.delete_hotspot),
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {

											hotSpots.remove(index);
											hotspotIconForAdd.remove(index);
											if (myView.equals(selectedImg))
												imageLevelFlag = 0;
											else {
												rootView.removeViewInLayout(myView);
												Log.i("After remove ",
														hotSpots.size() + "");
											}
											handleEventBeforeFurtherAction();
											dlg.dismiss();
											dialog.cancel();
										}
									})
							.setNeutralButton(
									getResources().getString(
											R.string.delete_properties),
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											hotSpots.get(index)
													.setInputText("");
											hotSpots.get(index).setInputUrl("");
											hotSpots.get(index).setAudioFile(
													null);
											handleEventBeforeFurtherAction();
											dlg.dismiss();
											dialog.cancel();
											removeDialog(100);
											showDialog(100);
										}
									})
							.setNegativeButton(android.R.string.cancel,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											handleEventBeforeFurtherAction();
											dlg.dismiss();
											dialog.cancel();
										}
									});
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
				}
			});

			backButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					handleEventBeforeFurtherAction();
					dlg.dismiss();
				}
			});

			videoButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(
							AddHotspotToImage.this,
							getResources().getString(R.string.video_annotation),
							Toast.LENGTH_LONG).show();
				}
			});

			locationButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(AddHotspotToImage.this,
							getResources().getString(R.string.poi_location),
							Toast.LENGTH_LONG).show();
				}
			});

			shoppingButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(AddHotspotToImage.this,
							getResources().getString(R.string.shopping_links),
							Toast.LENGTH_LONG).show();
				}
			});

			contactsButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(
							AddHotspotToImage.this,
							getResources().getString(
									R.string.assign_to_contacts),
							Toast.LENGTH_LONG).show();
				}
			});

			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (isAnyChange()) {
						AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
								AddHotspotToImage.this);
						alertDialogBuilder.setTitle(getResources().getString(
								R.string.hotspot));
						// set dialog message
						alertDialogBuilder
								.setMessage(
										getResources().getString(
												R.string.hotspot_save))
								.setCancelable(false)
								.setPositiveButton(android.R.string.yes,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int id) {
												saveTheChange();
												dialog.cancel();
											}
										})
								.setNegativeButton(android.R.string.no,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int id) {
												dialog.cancel();
											}
										});
						AlertDialog alertDialog = alertDialogBuilder.create();
						alertDialog.show();
					}
					dlg.dismiss();
				}
			});
			dlg = builder.create();
			Window window = dlg.getWindow();
			WindowManager.LayoutParams wlp = window.getAttributes();
			wlp.gravity = Gravity.BOTTOM;
			wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
			window.setAttributes(wlp);

			return dlg;
		}
		return super.onCreateDialog(id);
	}

	private void startPlayingRecordedAudio() {
		isPlaying = true;
		if (mediaPlayer != null) {
			mediaPlayer.release();
		}
		mediaPlayer = new MediaPlayer();
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(audioFilePath);
			mediaPlayer.setDataSource(fileInputStream.getFD());
			mediaPlayer.prepare();
			mediaPlayer.start();
			Log.v(AUDIO_SERVICE, "recording being played");
			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					if (isPlaying && mediaPlayer != null) {
						stopPlayingRecordedAudio();
						recordButton.setImageResource(R.drawable.play);
						Log.v(AUDIO_SERVICE, "recording play completed");
					}
				}
			});
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void stopPlayingRecordedAudio() {
		try {
			mediaPlayer.stop();
			mediaPlayer.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mediaPlayer = null;
		isPlaying = false;
	}

	private void startRecordAudio() {
		isRecording = true;
		if (mediaRecorder != null) {
			mediaRecorder.release();
		}
		mediaRecorder = new MediaRecorder();
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		audioFilePath = Utility.getRecordedAudio(this);
		mediaRecorder.setOutputFile(audioFilePath);
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		try {
			mediaRecorder.prepare();
			mediaRecorder.start();
			Log.v(AUDIO_SERVICE, "record is happening");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void stopRecordAudio() {
		try {
			mediaRecorder.stop();
			mediaRecorder.release();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		mediaRecorder = null;
		isRecording = false;
		canPlay = true;
		canRecord = false;
	}

	private void reInitMedia() {
		canPlay = false;
		canRecord = true;
		isPlaying = false;
		isRecording = false;
	}

	private void saveTheChange() {
		hotSpots.get(index).setInputText(textEdit.getText().toString());
		urlData = urlEdit.getText().toString();

		if (urlData != null && !urlData.isEmpty()) {
			if (Utility.isValidUrl(urlData)) {
				hotSpots.get(index).setInputUrl(urlData);
			} else {
				Toast.makeText(AddHotspotToImage.this,
						getResources().getString(R.string.invalid_url),
						Toast.LENGTH_LONG).show();
				urlEdit.setFocusable(true);
			}
		} else {
			hotSpots.get(index).setInputUrl(urlData);
		}
		if (audioFilePath != null) {
			hotSpots.get(index).setAudioFile(audioFilePath);
		} else if (selectedMediaPath != null) {
			hotSpots.get(index).setAudioFile(selectedMediaPath);
		}
		hotSpots.get(index).setMediaTypeFlag(mediaIdFlag);
		handleEventBeforeFurtherAction();
		dlg.dismiss();
	}

	private void drawAvailableHotspot() {
		hotSpots.addAll(dbHelper.getAllHotspot(new Image(imageFilePath)));
		if (!hotSpots.isEmpty()) {
			for (Hotspot h : hotSpots) {
				if (h.getXPosition() != 0 && h.getYPosition() != 0) {
					ImageView hotspot = new ImageView(this);
					hotspot.setImageResource(R.drawable.boundary);
					RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
							48, 48);
					layoutParams.leftMargin = (int) h.getXPosition()
							- HotspotDetails.densityDpi / 10;
					layoutParams.topMargin = (int) h.getYPosition()
							- HotspotDetails.densityDpi / 10;
					hotspot.setLayoutParams(layoutParams);
					hotspot.setOnTouchListener(this);
					hotspotIconForAdd.add(hotspot);
					hotspot.setOnLongClickListener(new OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							myView = v;
							removeDialog(100);
							showDialog(100);
							return false;
						}
					});
					rootView.addView(hotspot);
				} else if (h.getXPosition() == 0 && h.getYPosition() == 0) {
					hotspotIconForAdd.add(selectedImg);
					addButton.setOnLongClickListener(this);
					imageLevelFlag = 1;
				}
			}
		} else {
			Toast.makeText(AddHotspotToImage.this,
					getResources().getString(R.string.no_hotspot),
					Toast.LENGTH_LONG).show();
		}
	}

	private boolean isAnyChange() {
		if (text.equals(textEdit.getText().toString())
				&& url.equals(urlEdit.getText().toString())) {
			return false;
		}
		return true;
	}

	private void handleEventBeforeFurtherAction() {
		if (isRecording && mediaRecorder != null) {
			stopRecordAudio();
			recordButton.setImageResource(R.drawable.microphone_icon);
			Log.v(AUDIO_SERVICE, "RStopped due to clicking save button");

		} else if (isPlaying && mediaPlayer != null) {
			stopPlayingRecordedAudio();
			recordButton.setImageResource(R.drawable.play);
			Log.v(AUDIO_SERVICE, "PStopped due to clicking save button");
		}
		audioFilePath = null;
		selectedMediaPath = null;
		reInitMedia();
	}

	private void startActivityToChooseAudio() {
		Intent intent = new Intent();
		intent.setType("audio/mp3");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(
				Intent.createChooser(intent, "Open Audio (mp3) file"),
				Constants.RQS_OPEN_AUDIO_MP3);
	}

	private void startHotspotDetailsActivity() {
		Intent intent = new Intent(AddHotspotToImage.this, HotspotDetails.class);
		intent.putExtra("imageName", imageFilePath);
		startActivity(intent);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("imageName", imageFilePath);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		imageFilePath = savedInstanceState.getString("imageName");
	}
}
