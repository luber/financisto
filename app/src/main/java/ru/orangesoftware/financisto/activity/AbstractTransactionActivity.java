/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.db.DatabaseHelper.*;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.recur.NotificationOptions;
import ru.orangesoftware.financisto.recur.Recurrence;
import ru.orangesoftware.financisto.utils.*;
import ru.orangesoftware.financisto.view.AttributeView;
import ru.orangesoftware.financisto.view.AttributeViewFactory;
import ru.orangesoftware.financisto.widget.RateLayoutView;

import java.io.File;
import java.text.DateFormat;
import java.util.*;

import static ru.orangesoftware.financisto.utils.ThumbnailUtil.*;
import static ru.orangesoftware.financisto.utils.Utils.text;

public abstract class AbstractTransactionActivity extends AbstractActivity implements CategorySelector.CategorySelectorListener {
	
	public static final String TRAN_ID_EXTRA = "tranId";
	public static final String ACCOUNT_ID_EXTRA = "accountId";
	public static final String DUPLICATE_EXTRA = "isDuplicate";
	public static final String TEMPLATE_EXTRA = "isTemplate";
    public static final String DATETIME_EXTRA = "dateTimeExtra";
    public static final String NEW_FROM_TEMPLATE_EXTRA = "newFromTemplateExtra";

	private static final int NEW_LOCATION_REQUEST = 4002;
	private static final int RECURRENCE_REQUEST = 4003;
	private static final int NOTIFICATION_REQUEST = 4004;
	private static final int PICTURE_REQUEST = 4005;

	private static final TransactionStatus[] statuses = TransactionStatus.values();
	
    protected RateLayoutView rateView;

    protected EditText templateName;
	protected TextView accountText;	
	protected Cursor accountCursor;
	protected ListAdapter accountAdapter;
	
	protected TextView locationText;
	protected Cursor locationCursor;
	protected ListAdapter locationAdapter;

	protected Calendar dateTime;
	protected ImageButton status;
	protected Button dateText;
	protected Button timeText;
	
    protected EditText noteText;
	protected TextView recurText;	
	protected TextView notificationText;	
	
	private ImageView pictureView;
	private String pictureFileName;
	
	private CheckBox ccardPayment;
	
	protected Account selectedAccount;

	protected long selectedLocationId = 0;
	protected String recurrence;
	protected String notificationOptions;
	
	private LocationManager locationManager;
	private Location lastFix;

    protected boolean isDuplicate = false;
	
	private boolean setCurrentLocation;

    protected ProjectSelector projectSelector;
    protected CategorySelector categorySelector;

    protected boolean isRememberLastAccount;
	protected boolean isRememberLastCategory;
	protected boolean isRememberLastLocation;
	protected boolean isRememberLastProject;
	protected boolean isShowLocation;
	protected boolean isShowNote;
	protected boolean isShowTakePicture;
	protected boolean isShowIsCCardPayment;
	protected boolean isOpenCalculatorForTemplates;

    protected boolean isShowPayee = true;
    protected AutoCompleteTextView payeeText;
    protected SimpleCursorAdapter payeeAdapter;

	protected AttributeView deleteAfterExpired;
	
	protected DateFormat df;
	protected DateFormat tf;
	
	protected Transaction transaction = new Transaction();

    public AbstractTransactionActivity() {}
	
	protected abstract int getLayoutId();

//	@Override
//	protected void onNewIntent(Intent newIntent) {
//		this.setIntent(newIntent);
//	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		df = DateUtils.getLongDateFormat(this);
		tf = DateUtils.getTimeFormat(this);
		
		long t0 = System.currentTimeMillis();
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(getLayoutId());
		
		isRememberLastAccount = MyPreferences.isRememberAccount(this);
		isRememberLastCategory = isRememberLastAccount && MyPreferences.isRememberCategory(this);
		isRememberLastLocation = isRememberLastCategory && MyPreferences.isRememberLocation(this);
		isRememberLastProject = isRememberLastCategory && MyPreferences.isRememberProject(this);
		isShowLocation = MyPreferences.isShowLocation(this);
		isShowNote = MyPreferences.isShowNote(this);
		isShowTakePicture = MyPreferences.isShowTakePicture(this);
		isShowIsCCardPayment = MyPreferences.isShowIsCCardPayment(this);
        isOpenCalculatorForTemplates = MyPreferences.isOpenCalculatorForTemplates(this);

        categorySelector = new CategorySelector(this, db, x);
        categorySelector.setListener(this);
        fetchCategories();

        projectSelector = new ProjectSelector(this, em, x);
        projectSelector.fetchProjects();

		if (isShowLocation) {
			locationCursor = em.getAllLocations(true);
			startManagingCursor(locationCursor);
			locationAdapter = TransactionUtils.createLocationAdapter(this, locationCursor);
		}

		long accountId = -1;
		long transactionId = -1;
        boolean isNewFromTemplate = false;
		final Intent intent = getIntent();
		if (intent != null) {
			accountId = intent.getLongExtra(ACCOUNT_ID_EXTRA, -1);
			transactionId = intent.getLongExtra(TRAN_ID_EXTRA, -1);
            transaction.dateTime = intent.getLongExtra(DATETIME_EXTRA, System.currentTimeMillis());
			if (transactionId != -1) {
				transaction = db.getTransaction(transactionId);
                transaction.categoryAttributes = db.getAllAttributesForTransaction(transactionId);
				isDuplicate = intent.getBooleanExtra(DUPLICATE_EXTRA, false);
                isNewFromTemplate = intent.getBooleanExtra(NEW_FROM_TEMPLATE_EXTRA, false);
				if (isDuplicate) {
					transaction.id = -1;
					transaction.dateTime = System.currentTimeMillis();
				}
			}	
			transaction.isTemplate = intent.getIntExtra(TEMPLATE_EXTRA, transaction.isTemplate);
		}

		if (transaction.id == -1) {
			accountCursor = em.getAllActiveAccounts();
		} else {
			accountCursor = em.getAccountsForTransaction(transaction);
		}
		startManagingCursor(accountCursor);
		accountAdapter = TransactionUtils.createAccountAdapter(this, accountCursor);		
		
		dateTime = Calendar.getInstance();
		Date date = dateTime.getTime();

		status = (ImageButton)findViewById(R.id.status);
		status.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(AbstractTransactionActivity.this, statuses);
				x.selectPosition(AbstractTransactionActivity.this, R.id.status, R.string.transaction_status, adapter, transaction.status.ordinal());
			}
		});
		
		dateText = (Button)findViewById(R.id.date);
		dateText.setText(df.format(date));
		dateText.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				DatePickerDialog d = new DatePickerDialog(AbstractTransactionActivity.this, new OnDateSetListener(){
					@Override
					public void onDateSet(DatePicker arg0, int y, int m, int d) {
						dateTime.set(y, m, d);
						dateText.setText(df.format(dateTime.getTime()));
					}					
				}, dateTime.get(Calendar.YEAR), dateTime.get(Calendar.MONTH), dateTime.get(Calendar.DAY_OF_MONTH));
				d.show();
			}			
		});

		timeText = (Button)findViewById(R.id.time);
		timeText.setText(tf.format(date));
		timeText.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				boolean is24Format = DateUtils.is24HourFormat(AbstractTransactionActivity.this);
				TimePickerDialog d = new TimePickerDialog(AbstractTransactionActivity.this, new OnTimeSetListener(){
					@Override
					public void onTimeSet(TimePicker picker, int h, int m) {
						dateTime.set(Calendar.HOUR_OF_DAY, picker.getCurrentHour());
						dateTime.set(Calendar.MINUTE, picker.getCurrentMinute());
						timeText.setText(tf.format(dateTime.getTime()));
					}
				}, dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE), is24Format);
				d.show();
			}			
		});
			
		internalOnCreate();
		
		LinearLayout layout = (LinearLayout)findViewById(R.id.list);
		
		this.templateName = new EditText(this);
		if (transaction.isTemplate()) {
			x.addEditNode(layout, R.string.template_name, templateName);
		}

        rateView = new RateLayoutView(this, x, layout);

        createListNodes(layout);
		categorySelector.createAttributesLayout(layout);
		createCommonNodes(layout);
		
		if (transaction.isScheduled()) {
			recurText = x.addListNode(layout, R.id.recurrence_pattern, R.string.recur, R.string.recur_interval_no_recur);
			notificationText = x.addListNode(layout, R.id.notification, R.string.notification, R.string.notification_options_default);
			Attribute sa = db.getSystemAttribute(SystemAttribute.DELETE_AFTER_EXPIRED);
			deleteAfterExpired = AttributeViewFactory.createViewForAttribute(this, sa);
			String value = transaction.getSystemAttribute(SystemAttribute.DELETE_AFTER_EXPIRED);
			deleteAfterExpired.inflateView(layout, value != null ? value : sa.defaultValue);
		}

        Button bSave = (Button) findViewById(R.id.bSave);
		bSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                saveAndFinish();
            }

        });

        final boolean isEdit = transaction.id > 0;
		Button bSaveAndNew = (Button)findViewById(R.id.bSaveAndNew);
        if (isEdit) {
            bSaveAndNew.setText(R.string.cancel);
        }
		bSaveAndNew.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (isEdit) {
                    setResult(RESULT_CANCELED);
                    finish();
                } else {
                    if (saveAndFinish()) {
                        intent.putExtra(DATETIME_EXTRA, transaction.dateTime);
                        startActivityForResult(intent, -1);
                    }
                }
            }
        });
		
		if (transactionId != -1) {
            isOpenCalculatorForTemplates &= isNewFromTemplate;
			editTransaction(transaction);
		} else {
            setDateTime(transaction.dateTime);
            categorySelector.selectCategory(0);
            if (accountId != -1) {
                selectAccount(accountId);
            } else {
                long lastAccountId = MyPreferences.getLastAccount(this);
                if (isRememberLastAccount && lastAccountId != -1) {
                    selectAccount(lastAccountId);
                }
            }
			if (!isRememberLastProject) {
				projectSelector.selectProject(0);
			}
			if (!isRememberLastLocation) {
				selectCurrentLocation(false);
			}							
			if (transaction.isScheduled()) {
				selectStatus(TransactionStatus.PN);
			}
		}
		
		long t1 = System.currentTimeMillis();
		Log.i("TransactionActivity", "onCreate "+(t1-t0)+"ms");
	}

    protected void createPayeeNode(LinearLayout layout) {
        payeeAdapter = TransactionUtils.createPayeeAdapter(this, db);
        payeeText = new AutoCompleteTextView(this);
        payeeText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_TEXT_VARIATION_FILTER);
        payeeText.setThreshold(1);
        payeeText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    payeeText.setAdapter(payeeAdapter);
                    payeeText.selectAll();
                }
            }
        });
        x.addEditNode(layout, R.string.payee, payeeText);
    }

    protected abstract void fetchCategories();

    private boolean saveAndFinish() {
        long id = save();
        if (id > 0) {
            Intent data = new Intent();
            data.putExtra(TransactionColumns._id.name(), id);
            setResult(RESULT_OK, data);
            finish();
            return true;
        }
        return false;
    }

    private long save() {
        if (onOKClicked()) {
            boolean isNew = transaction.id == -1;
            long id = db.insertOrUpdate(transaction, getAttributes());
            if (isNew) {
                MyPreferences.setLastAccount(this, transaction.fromAccountId);
            }
            AccountWidget.updateWidgets(this);
            return id;
        }
        return -1;
    }

    private List<TransactionAttribute> getAttributes() {
        List<TransactionAttribute> attributes = categorySelector.getAttributes();
        if (deleteAfterExpired != null) {
            TransactionAttribute ta = deleteAfterExpired.newTransactionAttribute();
            attributes.add(ta);
        }
        return attributes;
    }

    protected void internalOnCreate() {
	}

	protected void selectCurrentLocation(boolean forceUseGps) {
        setCurrentLocation = true;
        selectedLocationId = 0;

		if (transaction.isTemplateLike()) {
			if (isShowLocation) {
				locationText.setText(R.string.current_location);
			}
			return;
		}		
		      
        // Start listener to find current location
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        String provider = locationManager.getBestProvider(new Criteria(), true);
        
        if (provider != null) {
        	lastFix = locationManager.getLastKnownLocation(provider);        	
        }  

        if (lastFix != null) {
        	setLocation(lastFix);
        	connectGps(forceUseGps);
        } else {
        	// No enabled providers found, so disable option
        	if (isShowLocation) {
        		locationText.setText(R.string.no_fix);
        	}
        }
	}

	private void connectGps(boolean forceUseGps) {
		if (locationManager != null) {
			boolean useGps = forceUseGps || MyPreferences.isUseGps(this);
            try {
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {	    	        
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkLocationListener);	        	    	        
                }
            } catch (Exception e) {
                Log.e("Financisto", "Unable to connect network provider");
            }
            try {
                if (useGps && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, gpsLocationListener);
                }
            } catch (Exception e) {
                Log.e("Financisto", "Unable to connect gps provider");
            }
		}
	}

	private void disconnectGPS() {
		if (locationManager != null) {
			locationManager.removeUpdates(networkLocationListener);
			locationManager.removeUpdates(gpsLocationListener);
		}
	}

	@Override
	protected void onDestroy() {
		disconnectGPS();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		disconnectGPS();
		super.onPause();
	}

    @Override
    protected boolean shouldLock() {
        return MyPreferences.isPinProtectedNewTransaction(this);
    }

    @Override
	protected void onResume() {
		super.onResume();
		if (lastFix != null) {
			connectGps(false);
		}
	}

	private class DefaultLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			Log.i(">>>>>>>>>", "onLocationChanged "+location.toString());
			lastFix = location;
			if (setCurrentLocation) {
				setLocation(location);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
		
	}

	private final LocationListener networkLocationListener = new DefaultLocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			super.onLocationChanged(location);
			locationManager.removeUpdates(networkLocationListener);
		}

	};
	
	private final LocationListener gpsLocationListener = new DefaultLocationListener();

	protected void createCommonNodes(LinearLayout layout) {
		int locationOrder = MyPreferences.getLocationOrder(this);
		int noteOrder = MyPreferences.getNoteOrder(this);
		int projectOrder = MyPreferences.getProjectOrder(this);
		for (int i=0; i<6; i++) {
			if (i == locationOrder) {
				if (isShowLocation) {
					//location
					locationText = x.addListNodePlus(layout, R.id.location, R.id.location_add, R.string.location, R.string.select_location);
				}
			}
			if (i == noteOrder) {
				if (isShowNote) {
					//note
					noteText = new EditText(this);
					x.addEditNode(layout, R.string.note, noteText);
				}
			}
			if (i == projectOrder) {
                projectSelector.createNode(layout);
			}
		}
		if (isShowTakePicture && transaction.isNotTemplateLike()) {
			pictureView = x.addPictureNodeMinus(this, layout, R.id.attach_picture, R.id.delete_picture, R.string.attach_picture, R.string.new_picture);
		}
		if (isShowIsCCardPayment) {
			// checkbox to register if the transaction is a credit card payment. 
			// this will be used to exclude from totals in bill preview
			ccardPayment = x.addCheckboxNode(layout, R.id.is_ccard_payment,
					R.string.is_ccard_payment, R.string.is_ccard_payment_summary, false);
		}
	}

    protected abstract void createListNodes(LinearLayout layout);
	
	protected abstract boolean onOKClicked();

	@Override
	protected void onClick(View v, int id) {
        projectSelector.onClick(id);
        categorySelector.onClick(id);
		switch(id) {
			case R.id.account:				
				x.select(this, R.id.account, R.string.account, accountCursor, accountAdapter,
                        AccountColumns.ID, getSelectedAccountId());
				break;
			case R.id.location: {
				x.select(this, R.id.location, R.string.location, locationCursor, locationAdapter, "_id", selectedLocationId);
				break;
			}
			case R.id.location_add: {
				Intent intent = new Intent(this, LocationActivity.class);
				startActivityForResult(intent, NEW_LOCATION_REQUEST);				
				break;
			}
			case R.id.recurrence_pattern: {
				Intent intent = new Intent(this, RecurrenceActivity.class);
				intent.putExtra(RecurrenceActivity.RECURRENCE_PATTERN, recurrence);
				startActivityForResult(intent, RECURRENCE_REQUEST);				
				break;
			}
			case R.id.notification: {
				Intent intent = new Intent(this, NotificationOptionsActivity.class);
				intent.putExtra(NotificationOptionsActivity.NOTIFICATION_OPTIONS, notificationOptions);
				startActivityForResult(intent, NOTIFICATION_REQUEST);				
				break;
			}
			case R.id.attach_picture: {
				PICTURES_DIR.mkdirs();
				PICTURES_THUMB_DIR.mkdirs();				
				pictureFileName = PICTURE_FILE_NAME_FORMAT.format(new Date())+".jpg";
				transaction.blobKey=null;		
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, 
						Uri.fromFile(new File(PICTURES_DIR, pictureFileName)));
				startActivityForResult(intent, PICTURE_REQUEST);
				break;
			}
			case R.id.delete_picture: {
				removePicture();
				break;
			}
			case R.id.is_ccard_payment: {
				ccardPayment.setChecked(!ccardPayment.isChecked());
				transaction.isCCardPayment = ccardPayment.isChecked()?1:0;
			}
		}
	}	

	@Override
	public void onSelectedPos(int id, int selectedPos) {
        projectSelector.onSelectedPos(id, selectedPos);
		switch(id) {
			case R.id.status:
				selectStatus(statuses[selectedPos]);
				break;
		}
	}
 
	@Override
	public void onSelectedId(int id, long selectedId) {
        categorySelector.onSelectedId(id, selectedId);
		switch(id) {
			case R.id.account:				
				selectAccount(selectedId);
				break;
			case R.id.location:
				selectLocation(selectedId);
				break;
		}
	}
	
	private void selectStatus(TransactionStatus transactionStatus) {
		transaction.status = transactionStatus;
		status.setImageResource(transactionStatus.iconId);
	}

	protected Account selectAccount(long accountId) {
		return selectAccount(accountId, true);
	}
	
	protected Account selectAccount(long accountId, boolean selectLast) {
        Account a = em.getAccount(accountId);
        if (a != null) {
            accountText.setText(a.title);
            rateView.selectCurrencyFrom(a.currency);
            selectedAccount = a;
        }
        return a;
	}

    protected long getSelectedAccountId() {
        return selectedAccount != null ? selectedAccount.id : -1;
    }

    @Override
    public void onCategorySelected(Category category, boolean selectLast) {
        addOrRemoveSplits();
        categorySelector.addAttributes(transaction);
        switchIncomeExpenseButton(category);
        if (selectLast && isRememberLastLocation) {
            selectLocation(category.lastLocationId);
        }
        if (selectLast && isRememberLastProject) {
            projectSelector.selectProject(category.lastProjectId);
        }
        projectSelector.setProjectNodeVisible(!category.isSplit());
    }

    protected void addOrRemoveSplits() {
    }

    protected void switchIncomeExpenseButton(Category category) {

    }

	private void selectLocation(long locationId) {
		if (locationId == 0) {
			selectCurrentLocation(false);
		} else {
			if (isShowLocation) {
                MyLocation location = em.get(MyLocation.class, locationId);
				if (location != null) {
					locationText.setText(location.toString());
					selectedLocationId = locationId;
					setCurrentLocation = false;
				}
			}
		}
	}

	private void setRecurrence(String recurrence) {
		this.recurrence = recurrence;
		if (recurrence == null) {
			recurText.setText(R.string.recur_interval_no_recur);
			dateText.setEnabled(true);
			timeText.setEnabled(true);
		} else {			
			dateText.setEnabled(false);
			timeText.setEnabled(false);
			Recurrence r = Recurrence.parse(recurrence);
			recurText.setText(r.toInfoString(this));
		}
	}

	private void setNotification(String notificationOptions) {
		this.notificationOptions = notificationOptions;
		if (notificationOptions == null) {
			notificationText.setText(R.string.notification_options_default);
		} else {			
			NotificationOptions o = NotificationOptions.parse(notificationOptions);
			notificationText.setText(o.toInfoString(this));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
        projectSelector.onActivityResult(requestCode, resultCode, data);
        categorySelector.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
            rateView.onActivityResult(requestCode, data);
			switch (requestCode) {
				case NEW_LOCATION_REQUEST:
					locationCursor.requery();
					long locationId = data.getLongExtra(LocationActivity.LOCATION_ID_EXTRA, -1);
					if (locationId != -1) {
						selectLocation(locationId);
					}
					break;
				case RECURRENCE_REQUEST:					
					String recurrence = data.getStringExtra(RecurrenceActivity.RECURRENCE_PATTERN);
					setRecurrence(recurrence);
					break;
				case NOTIFICATION_REQUEST:					
					String notificationOptions = data.getStringExtra(NotificationOptionsActivity.NOTIFICATION_OPTIONS);
					setNotification(notificationOptions);
					break;
				case PICTURE_REQUEST:
					selectPicture(pictureFileName);	
					break;
				default:
					break;
			}
		} else {
			if (requestCode == PICTURE_REQUEST) {
				removePicture();
			}
		}
	}
	
	private void selectPicture(String pictureFileName) {
		if (pictureView == null) {
			return;
		}
		if (pictureFileName == null) {
			return;
		}
		File pictureFile = new File(PICTURES_DIR, pictureFileName);
		if (pictureFile.exists()) {
			Bitmap thumb = createThumbnail(pictureFile);					
			pictureView.setImageBitmap(thumb);
			pictureView.setTag(pictureFileName);
			transaction.attachedPicture = pictureFileName;
			
		}				
	}
	
	private void removePicture() {
		if (pictureView == null) {
			return;
		}
		if (pictureFileName != null) {
			new File(PICTURES_DIR, pictureFileName).delete();
			new File(PICTURES_THUMB_DIR, pictureFileName).delete();
		}		
		pictureFileName = null;
		transaction.attachedPicture = null;
		transaction.blobKey=null;	
		pictureView.setImageBitmap(null);		
		pictureView.setTag(null);
	}

	private Bitmap createThumbnail(File pictureFile) {
		return createAndStoreImageThumbnail(getContentResolver(), pictureFile);
	}

	protected void setDateTime(long date) {
		Date d = new Date(date);
		dateTime.setTime(d);
		dateText.setText(df.format(d));
		timeText.setText(tf.format(d));		
	}

    protected abstract void editTransaction(Transaction transaction);

	protected void commonEditTransaction(Transaction transaction) {
		selectStatus(transaction.status);
		categorySelector.selectCategory(transaction.categoryId, false);
		projectSelector.selectProject(transaction.projectId);
		setDateTime(transaction.dateTime);		
		if (transaction.locationId > 0) {
			selectLocation(transaction.locationId);
		} else {
			setLocation(transaction.provider, transaction.accuracy, transaction.latitude, transaction.longitude);
		}
		if (isShowNote) {
			noteText.setText(transaction.note);
		}
		if (transaction.isTemplate()) {
			templateName.setText(transaction.templateName);
		}
		if (transaction.isScheduled()) {
			setRecurrence(transaction.recurrence);
			setNotification(transaction.notificationOptions);
		}
		if (isShowTakePicture) {
			selectPicture(transaction.attachedPicture);
		}
		if (isShowIsCCardPayment) {
			setIsCCardPayment(transaction.isCCardPayment);
		}

        if (transaction.isCreatedFromTemlate()&& isOpenCalculatorForTemplates ){
            rateView.openFromAmountCalculator();
        }
	}

	private void setIsCCardPayment(int isCCardPaymentValue) {
		transaction.isCCardPayment = isCCardPaymentValue;
		ccardPayment.setChecked(isCCardPaymentValue==1);
	}

	private void setLocation(String provider, float accuracy, double latitude, double longitude) {
		lastFix = new Location(provider);
		lastFix.setLatitude(latitude);
		lastFix.setLongitude(longitude);
		lastFix.setAccuracy(accuracy);
		setLocation(lastFix);
	}

	private void setLocation(Location lastFix) {
		if (isShowLocation) {
			if (lastFix.getProvider() == null) {
				locationText.setText(R.string.no_fix);
			} else {
				locationText.setText(Utils.locationToText(lastFix.getProvider(), 
					lastFix.getLatitude(), lastFix.getLongitude(), 
					lastFix.hasAccuracy() ? lastFix.getAccuracy() : 0, null));
			}
		}
	}

	protected void updateTransactionFromUI(Transaction transaction) {
		transaction.categoryId = categorySelector.getSelectedCategoryId();
		transaction.projectId = projectSelector.getSelectedProjectId();
		if (transaction.isScheduled()) {
			DateUtils.zeroSeconds(dateTime);
		}
		transaction.dateTime = dateTime.getTime().getTime();
		if (selectedLocationId > 0) {
			transaction.locationId = selectedLocationId;
		} else {
			transaction.locationId = 0;
			transaction.provider = lastFix != null ? lastFix.getProvider() : null;
			transaction.accuracy = lastFix != null ? lastFix.getAccuracy() : 0;
			transaction.latitude = lastFix != null ? lastFix.getLatitude() : 0;
			transaction.longitude = lastFix != null ? lastFix.getLongitude() : 0;
		}
        if (isShowPayee) {
            transaction.payeeId = db.insertPayee(text(payeeText));
        }
		if (isShowNote) {
			transaction.note = text(noteText);
		}
		if (transaction.isTemplate()) {
			transaction.templateName = text(templateName);
		}
		if (transaction.isScheduled()) {
			transaction.recurrence = recurrence;
			transaction.notificationOptions = notificationOptions;
		}
	}

    protected void selectPayee(long payeeId) {
        if (isShowPayee) {
            Payee p = db.em().get(Payee.class, payeeId);
            selectPayee(p);
        }
    }

    protected void selectPayee(Payee p) {
        if (p != null) {
            payeeText.setText(p.title);
            transaction.payeeId = p.id;
        }
    }

}
