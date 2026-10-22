package io.coingift.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.google.zxing.common.BitMatrix;
import io.coingift.core.BackupFiles;
import io.coingift.core.CameraEntropy;
import io.coingift.core.WalletNote;
import io.coingift.core.BitcoinAddresses;
import io.coingift.core.CreationSession;
import io.coingift.core.WalletCrypto;
import io.coingift.core.PasswordStrength;
import io.coingift.core.NewPasswordPolicy;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Offline guided workflow. Only authenticated PUBLIC verification history is persisted. */
public final class MainActivity extends Activity implements NfcAdapter.ReaderCallback,DocumentIo.Listener,NfcIo.Listener,NfcReaderControl.Listener {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final WalletCrypto crypto=new WalletCrypto();
    private AppUi ui;
    private ApprovedCopy copy;private HelpPages helpPages;
    private String setupNote="",articleReturn="home",protectionReturn="home",entryTarget,lockStatus="";
    private boolean enhanced,lockAfterWrite,rendering,reopenLanguage;
    private int articleScroll;
    private CameraCapture camera;private CameraEntropy cameraEntropy;private Bitmap cameraPhoto;private ImageView cameraPhotoView;
    private static final int CAMERA_PERMISSION=24;
    @Override protected void attachBaseContext(android.content.Context base){super.attachBaseContext(Texts.wrap(base));}
    private String tr(String text){return Texts.tr(this,text);}

    private CreationSession session;
    private PrivateSession unlocked;
    private VerificationStore verificationStore;
    private VerificationStore.Record publicVerification,previousVerification,pendingVerification;
    private String verificationSource="",incomingCardId;
    private boolean recordLoading,recordReadFailed,recordSaveFailed,requestedPrivate;
    private CreationSession recordedCreation;
    private int recordedCreationCards;
    private String pendingAddressType;
    private java.util.List<VerificationStore.Record> recordRows=java.util.Collections.emptyList();
    private AccessibilityGate accessGate;
    private Dialog accessNotice;
    private volatile boolean privateOpening;
    private final java.util.Set<char[]> pendingPrivateViews=new java.util.HashSet<>();
    private final UnlockDeadline unlockDeadline=new UnlockDeadline();
    private Dialog autoLockDialog;
    private final Runnable checkUnlockDeadline=this::tickUnlockedSession;
    private NfcReaderControl nfcReader;
    private boolean readerPending;
    private final Runnable expireReaderStart=this::expireReader;
    private NfcCards.Candidate candidate;
    private String screen="home",incoming,exportData,exportAction;
    private boolean copyWriting,busy,fileBusy;
    private volatile boolean resumed;
    private DocumentIo.Job fileJob;
    private CreationSession fileOwner;
    private String fileThen,filePayload;
    private boolean filePrivate;
    private AlertDialog fileProgress,fileErrorDialog;
    private final Runnable expireFileTask=()->{if(fileJob!=null)cancelDocumentOperation(false);};
    private volatile int epoch;
    private volatile boolean nfcBusy;
    private NfcIo.Job nfcJob;
    private Mode nfcOperation=Mode.NONE;
    private int nfcToken;
    private CreationSession nfcOwner;
    private boolean nfcProtect;
    private final Runnable expireNfcTask=this::expireNfc;
    private volatile Mode mode=Mode.NONE;
    private enum Mode { NONE,CHECK,WRITE,READ,LOCK,VERIFY_LOCK }
    private static final int IMPORT=21,EXPORT=22,EXPORT_QR=23;
    private Dialog sheet;
    private NfcScanDialog nfcDialog;
    private Dialog workDialog;
    private Button workSubmit,creationSubmit,unlockSubmit;
    private String workLabel;private int workIcon;
    private final java.util.Map<EditText,CharSequence> workHints=new java.util.IdentityHashMap<>();
    private Dialog qrDialog,qrSaveConfirm,copyConfirm,weakPasswordConfirm,unlockDialog,creationDialog;
    private TextView unlockFeedback,creationFeedback;
    private TextView copyConfirmFeedback;
    private TextView qrFeedback;
    private byte[] pendingQrImage;
    private boolean pendingQrPrivate;
    private long pendingQrDeadline;
    private final Runnable expireQrExport=()->{if(pendingQrImage!=null){clearPendingQrImage();whenResumed(()->showDocumentError(true));}};
    private Bitmap qrBitmap;
    private ImageView qrImage;
    private PrivateKeyView secretView;
    private TextView feedback;
    private Button secretToggle;
    private Runnable afterResume;
    private String clipboardMarker,publicClipboardMarker;
    private final java.util.Set<String> ownedClipboardMarkers=new java.util.HashSet<>();
    private final java.util.Set<char[]> pendingPasswords=new java.util.HashSet<>();
    private long clipboardExpiry;
    private final Runnable hideSecret=()->{
        if(secretView!=null)secretView.clear();
        if(secretToggle!=null){secretToggle.setText(tr("显示"));secretToggle.setCompoundDrawablesRelative(ui.drawable(R.drawable.ic_eye_open,AppUi.ACCENT,14),null,null,null);}
    };
    private final Runnable clearPrivateClipboard=this::expirePrivateClipboard;

    /** Deliberately not Parcelable/Serializable: survives configuration changes in memory only. */
    private static final class RetainedFlow {
        CreationSession session;String screen,incoming,exportData,exportAction,clipboardMarker,publicClipboardMarker;
        boolean copyWriting,lockAfterWrite,enhanced,reopenLanguage;String lockStatus,setupNote,entryTarget,articleReturn,protectionReturn,incomingCardId;long clipboardExpiry;
        final java.util.Set<String> ownedClipboardMarkers;
        RetainedFlow(MainActivity owner){
            incomingCardId=owner.incomingCardId;
            protectionReturn=owner.protectionReturn;reopenLanguage=owner.reopenLanguage;enhanced=owner.enhanced;entryTarget=owner.entryTarget;articleReturn=owner.articleReturn;lockAfterWrite=owner.lockAfterWrite;lockStatus=owner.lockStatus;setupNote=owner.setupNote;session=owner.session;screen=owner.screen;incoming=owner.incoming;exportData=owner.exportData;exportAction=owner.exportAction;
            copyWriting=owner.copyWriting;clipboardMarker=owner.clipboardMarker;publicClipboardMarker=owner.publicClipboardMarker;clipboardExpiry=owner.clipboardExpiry;
            ownedClipboardMarkers=new java.util.HashSet<>(owner.ownedClipboardMarkers);
        }
        void restore(MainActivity owner){
            owner.incomingCardId=incomingCardId;
            owner.protectionReturn=protectionReturn;owner.reopenLanguage=reopenLanguage;owner.enhanced=enhanced;owner.entryTarget=entryTarget;owner.articleReturn=articleReturn;owner.lockAfterWrite=lockAfterWrite;owner.lockStatus=lockStatus;owner.setupNote=setupNote;owner.session=session;owner.screen=screen;owner.incoming=incoming;owner.exportData=exportData;owner.exportAction=exportAction;owner.copyWriting=copyWriting;
            owner.clipboardMarker=clipboardMarker;owner.publicClipboardMarker=publicClipboardMarker;owner.clipboardExpiry=clipboardExpiry;
            owner.ownedClipboardMarkers.addAll(ownedClipboardMarkers);
            clear();
        }
        void clear(){session=null;screen=null;incoming=null;incomingCardId=null;exportData=null;exportAction=null;clipboardMarker=null;publicClipboardMarker=null;copyWriting=false;clipboardExpiry=0;enhanced=false;lockAfterWrite=false;lockStatus=null;setupNote=null;entryTarget=null;articleReturn=null;protectionReturn=null;ownedClipboardMarkers.clear();}
    }
    @Override public void onCreate(Bundle state){
        if(state!=null)state.clear(); // Ignore legacy wallet state saved by earlier versions.
        super.onCreate(state);WindowProtection.secure(getWindow());
        getWindow().setDecorFitsSystemWindows(false);
        ui=new AppUi(this,()->busy||fileBusy||recordLoading||privateOpening||expireUnlockedIfNeeded(),this::tap);ui.language=this::language;ui.obscuredTouch=this::onObscuredTouch;ui.interaction=this::privateInteraction;copy=new ApprovedCopy(this);helpPages=new HelpPages(ui,copy);nfcReader=new NfcReaderControl(new NfcReaderService(this),this,handler::post);verificationStore=new VerificationStore(this);
        accessGate=new AccessibilityGate(this,handler,this::accessibilityChanged);
        worker.execute(()->{try{PrivateSession.cleanupStaleKeys();}catch(Exception ignored){/* Secret restoration fails closed if the keystore remains unavailable. */}});
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);
        Object retained=getLastNonConfigurationInstance();if(retained instanceof RetainedFlow)((RetainedFlow)retained).restore(this);
        render();if(reopenLanguage&&screen.equals("home")){reopenLanguage=false;language();}
    }
    @Override public Object onRetainNonConfigurationInstance(){return new RetainedFlow(this);}
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        // No wallet, password, private key, PNG data or UI hierarchy is serialized.
        state.clear();
    }
    @Override protected void onResume(){
        super.onResume();resumed=true;accessibilityChanged();expirePrivateClipboard();updateReader();
        if(clipboardMarker!=null){handler.removeCallbacks(clearPrivateClipboard);handler.postDelayed(clearPrivateClipboard,Math.max(0,clipboardExpiry-android.os.SystemClock.elapsedRealtime()));}
        if(afterResume!=null){Runnable next=afterResume;afterResume=null;next.run();}
        if(incoming!=null&&screen.equals("unlock")&&publicVerification==null&&!recordLoading)checkIncomingRecord();
        if(screen.equals("records"))loadRecords();
    }
    @Override protected void onPause(){
        closeAccessNotice();dismissAutoLockDialog();dismissFileError();cancelDocumentOperation(true);
        resumed=false;epoch++;busy=false;recordLoading=false;if(mode==Mode.LOCK||mode==Mode.VERIFY_LOCK)lockStatus="unknown";if(mode==Mode.WRITE||mode==Mode.CHECK)clearCandidate();stopNfcJob();mode=Mode.NONE;clearCamera();if(screen.equals("camera")||screen.equals("review"))screen="setup";
        if(nfcReader!=null)nfcReader.request(false,epoch);readerPending=false;handler.removeCallbacks(expireReaderStart);
        closeSheet();dismissNfc();dismissWork();dismissQr();ui.clearPasswords();wipePendingPasswords();closeUnlocked();hideSecret.run();
        if(screen.equals("revealed"))screen="unlock";
        super.onPause();
    }
    @Override protected void onStop(){super.onStop();if(!isFinishing())render();}
    @Override protected void onDestroy(){if(accessGate!=null)accessGate.close();clearWalletSession(false);if(nfcReader!=null)nfcReader.close();worker.shutdownNow();super.onDestroy();}
    private void whenResumed(Runnable action){if(isDestroyed()||isFinishing())return;if(resumed)action.run();else afterResume=action;}
    private void navigate(String next){
        if(busy||fileBusy)return;
        if(next.equals("home")&&session!=null&&!session.backupSaved()){backupSheet("home");return;}
        epoch++;stopNfcJob();mode=Mode.NONE;closeSheet();dismissNfc();dismissQr();updateReader();ui.clearPasswords();closeUnlocked();clearCandidate();
        hideKeyboard();screen=next;render();
        if(next.equals("read")){clearIncomingVerification();incoming=null;incomingCardId=null;}
        if(next.equals("unlock")&&incoming!=null&&publicVerification==null&&!recordLoading)checkIncomingRecord();
    }
    @Override public void onBackPressed(){back();}
    private void back(){
        if(fileBusy){cancelDocumentOperation(false);return;}
        if(busy)return;
        switch(screen){
            case "home":finish();break;
            case "setup":navigate("home");break;
            case "camera":case "review":clearCamera();navigate("setup");break;
            case "legal":{String target=entryTarget;screen="home";render();entryDisclaimer(target);break;}
            case "security":returnFromSecurity();break;
            case "verification":returnFromSecurity();break;
            case "records":navigate("verification");break;
            case "protection":returnFromProtection();break;
            case "write":if(copyWriting&&session!=null&&session.cardCount()>0){copyWriting=false;navigate("done");}else leaveCreation();break;
            case "done":leaveCreation();break;
            case "unlock":incoming=null;navigate("read");break;
            case "revealed":navigate("unlock");break;
            default:navigate("home");
        }
    }
    private void leaveCreation(){
        if(session!=null&&!session.backupSaved()){backupSheet("home");return;}
        navigate("home");
    }
    private void render(){
        closeCreationDialog();
        if(screen.equals("home"))clearWalletSession(true);
        rendering=true;ui.install();secretView=null;secretToggle=null;
        boolean home=screen.equals("home");
        boolean article=screen.equals("security")||screen.equals("verification")||screen.equals("records"),legal=screen.equals("legal");
        String toolbarTitle=legal?copy.ui("legalFull"):article?copy.ui("securityNav"):screen.equals("setup")||screen.equals("camera")||screen.equals("review")||screen.equals("write")||screen.equals("done")?copy.ui("make"):screen.equals("revealed")?copy.ui("revealedTitle"):copy.ui("read");
        if(screen.equals("verification"))toolbarTitle=getString(R.string.verification_entry);else if(screen.equals("records"))toolbarTitle=getString(R.string.verification_records_title);
        ui.toolbar(home,screen.equals("camera")?copy.ui("cameraNav"):toolbarTitle,this::back,article||legal||screen.equals("camera")?null:this::help);
        if(home||screen.equals("unlock")||screen.equals("revealed")||screen.equals("camera")){
            boolean compact=home&&ui.compactHome();
            boolean recovery=screen.equals("unlock")||screen.equals("revealed");
            View toolbar=ui.page.getChildAt(0);toolbar.getLayoutParams().height=ui.dp(compact||recovery&&compactRecovery()?44:52);
            ui.page.getChildAt(1).getLayoutParams().height=ui.dp(compact||recovery?4:8);
            ui.page.setPadding(ui.dp(24),ui.dp(home?(compact?4:8):recovery?8:12),ui.dp(24),ui.dp(home?(compact?12:20):recovery?12:18));
            if(recovery&&compactRecovery())ui.page.setPadding(ui.dp(24),ui.dp(4),ui.dp(24),ui.dp(8));
        }
        feedback=ui.text(ui.page,"",12,AppUi.ERROR,false);feedback.setVisibility(View.GONE);
        switch(screen){
            case "setup":setup();break;
            case "camera":cameraPage();break;
            case "review":if(cameraPhoto!=null&&cameraEntropy!=null)cameraReviewPage();else{screen="setup";setup();}break;
            case "legal":helpPages.terms();ui.action(ui.page,copy.ui("backDisclaimer"),true,R.drawable.ic_back,this::back);break;
            case "security":helpPages.security();ui.stretch();ui.action(ui.page,copy.ui("backSecurity"),true,R.drawable.ic_back,this::returnFromSecurity);break;
            case "verification":helpPages.verification();ui.action(ui.page,getString(R.string.verification_manage),false,R.drawable.ic_record_check,this::showRecords);ui.action(ui.page,copy.ui("backSecurity"),true,R.drawable.ic_back,this::returnFromSecurity);break;
            case "records":recordsPage();break;
            case "protection":protectionPage();break;
            case "write":if(session!=null)writing();else home();break;
            case "done":if(session!=null&&session.cardCount()>0)completed();else home();break;
            case "read":reading();break;
            case "unlock":if(incoming!=null)unlocking();else reading();break;
            case "revealed":if(unlocked!=null)revealed();else{screen="unlock";unlocking();}break;
            default:home();
        }
        rendering=false;
    }
    private void home(){
        screen="home";boolean compact=ui.compactHome();ui.space(ui.page,compact?0:10);LinearLayout eyebrow=ui.horizontal();ui.page.addView(eyebrow);ImageView wifi=new ImageView(this);wifi.setImageDrawable(ui.drawable(R.drawable.ic_wifi_off,AppUi.SUB,17));eyebrow.addView(wifi,new LinearLayout.LayoutParams(ui.dp(17),ui.dp(17)));TextView label=ui.text(eyebrow,copy.ui("offline"),12,AppUi.SUB,false);label.setPadding(ui.dp(7),0,0,0);label.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        ui.space(ui.page,compact?10:17);TextView title=ui.text(ui.page,copy.ui("hero"),compact?28:32,AppUi.FG,true);ui.line(title,compact?1.35f:1.45f);title.setLetterSpacing(-.032f);ui.space(ui.page,8);
        WalletArtView artwork=new WalletArtView(this,false);artwork.setTag("home-artwork");ui.page.addView(artwork,new LinearLayout.LayoutParams(-1,ui.dp(220)));
        Button make=ui.action(ui.page,copy.ui("make"),true,R.drawable.ic_plus,()->entryDisclaimer("setup"));((LinearLayout.LayoutParams)make.getLayoutParams()).topMargin=ui.dp(compact?12:20);homeFoot(copy.ui("makeSub"),8);
        Button read=ui.action(ui.page,copy.ui("read"),false,R.drawable.ic_nfc,()->entryDisclaimer("read"));((LinearLayout.LayoutParams)read.getLayoutParams()).topMargin=ui.dp(compact?14:20);homeFoot(copy.ui("readSub"),8);
        ui.fitHomeArtwork(artwork);
    }
    private void homeFoot(String value,int top){TextView text=ui.text(ui.page,value,12,AppUi.SUB,false);text.setGravity(Gravity.CENTER);ui.line(text,1.6f);text.setPadding(0,ui.dp(top),0,0);}
    private void setup(){
        ui.progress(1);ui.heading(copy.ui("setupStep"),copy.ui("setupTitle"),copy.ui("setupDesc"));
        LinearLayout prep=ui.horizontal();prep.setTag("supported-card-prep");prep.setPadding(ui.dp(12),ui.dp(8),ui.dp(12),ui.dp(8));prep.setMinimumHeight(ui.dp(60));prep.setBackground(ui.shape(AppUi.SOFT,13,false));ui.page.addView(prep,new LinearLayout.LayoutParams(-1,-2));
        ImageView prepIcon=new ImageView(this);prepIcon.setImageDrawable(ui.drawable(R.drawable.ic_nfc,AppUi.ACCENT,17));prepIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams prepIconParams=new LinearLayout.LayoutParams(ui.dp(17),ui.dp(17));prepIconParams.rightMargin=ui.dp(10);prep.addView(prepIcon,prepIconParams);
        TextView prepText=ui.text(prep,copy.section("cardHelpCopy").optString("prep"),12,AppUi.SUB,false);prepText.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);ui.line(prepText,1.6f);prepText.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        Button supportedCards=ui.textAction(prep,copy.section("cardHelpCopy").optString("view"),0,R.drawable.ic_chevron_right,this::cardHelp);
        supportedCards.setMaxWidth(ui.dp(140));supportedCards.setSingleLine(false);supportedCards.setEllipsize(null);
        ((LinearLayout.LayoutParams)supportedCards.getLayoutParams()).leftMargin=ui.dp(8);
        TextView count=new TextView(this);count.setTextSize(11);count.setTextColor(AppUi.SUB);count.setText(getString(R.string.note_counter,copy.ui("optional"),setupNote.codePointCount(0,setupNote.length())));count.setTag("note-count");
        EditText note=ui.input(copy.ui("remark"),false,count);note.setSingleLine(true);note.setMinLines(1);note.setMinHeight(ui.dp(52));note.setText(setupNote);
        note.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence x,int a,int c,int z){}public void onTextChanged(CharSequence x,int a,int c,int z){if(!rendering){setupNote=x.toString();count.setText(getString(R.string.note_counter,copy.ui("optional"),setupNote.codePointCount(0,setupNote.length())));}}public void afterTextChanged(android.text.Editable x){}});ui.helper(ui.page,copy.ui("remarkHint"));
        LinearLayout modeCard=ui.mode(ui.page);modeCard.setTag("camera-mode-card");ui.toggle(modeCard,copy.ui("cameraMode"),R.drawable.ic_camera,enhanced,on->{enhanced=on;clearCamera();render();});ui.helper(modeCard,copy.ui("cameraSub"));ui.helper(modeCard,copy.ui("cameraOptional"));
        if(enhanced&&cameraEntropy!=null)ui.text(modeCard,copy.ui("captureReady"),11,AppUi.ACCENT,false);
        ui.textAction(modeCard,copy.ui("principle"),0,R.drawable.ic_arrow_up_right,()->{closeSheet();sheet=helpPages.principle(this::closeSheet);});
        if(enhanced&&cameraEntropy!=null)ui.textAction(modeCard,copy.ui("retake"),0,R.drawable.ic_camera,this::requestCamera);
        ui.stretch();
        boolean ready=enhanced&&cameraEntropy!=null&&cameraEntropy.ready();
        String action=enhanced&&!ready?copy.ui("photoGenerate"):ready?copy.ui("generateWithPassword"):copy.ui("generate");
        ui.action(ui.page,action,true,enhanced&&!ready?R.drawable.ic_camera:R.drawable.ic_lock,()->{
            if(enhanced&&(cameraEntropy==null||!cameraEntropy.ready()))requestCamera();else showCreationDialog(note);
        });ui.foot(copy.ui("local"));
    }
    private void showCreationDialog(EditText note){
        if(!allowSecretOperation())return;
        if(!resumed||busy||fileBusy||!screen.equals("setup")||creationDialog!=null||!note.isAttachedToWindow())return;
        try{WalletNote.normalize(note.getText().toString());if(enhanced&&(cameraEntropy==null||!cameraEntropy.ready()))throw new IllegalArgumentException(copy.ui("captureBeforePassword"));}
        catch(IllegalArgumentException ex){error(ex.getMessage());return;}
        closeSheet();Dialog owner=new Dialog(this);creationDialog=owner;
        int[] revision={0};android.text.TextWatcher changed=new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence x,int start,int count,int after){}
            public void onTextChanged(CharSequence x,int start,int before,int count){revision[0]++;}
            public void afterTextChanged(android.text.Editable x){}
        };
        note.addTextChangedListener(changed);
        owner.setOnDismissListener(d->{
            if(busy&&workDialog==owner)cancelWork();
            closeWeakPasswordConfirm();note.removeTextChangedListener(changed);
            if(owner.getWindow()!=null)ui.releaseInputs(owner.getWindow().getDecorView());
            if(creationDialog==owner){creationDialog=null;creationFeedback=null;creationSubmit=null;}
        });
        ui.showSheet(owner,copy.ui("creationPasswordTitle"),p->{
            ui.note(p,copy.ui("creationPasswordDescription"));
            EditText first=ui.input(p,copy.ui("pass"),true,null);
            ui.helper(p,copy.ui("passHint"));ui.passwordRequirements(p,first);
            EditText second=ui.input(p,copy.ui("confirm"),true,null);
            first.addTextChangedListener(changed);second.addTextChangedListener(changed);
            creationFeedback=ui.text(p,"",12,AppUi.ERROR,false);creationFeedback.setVisibility(View.GONE);creationFeedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            TextView feedback=creationFeedback;
            ui.callout(p,R.drawable.ic_key,copy.ui("passwordNote"));ui.space(p,14);
            Button generate=ui.action(p,copy.ui("creationConfirmGenerate"),true,R.drawable.ic_lock,()->requestWalletGeneration(first,second,note,revision));
            creationSubmit=generate;
            Runnable validate=()->{
                if(busy||creationDialog!=owner)return;
                NewPasswordPolicy.Requirements requirements=NewPasswordPolicy.inspect(first.getText());
                boolean format=requirements.satisfied();
                boolean characters=requirements.characters&&NewPasswordPolicy.inspect(second.getText()).characters;
                boolean same=second.length()>0&&android.text.TextUtils.equals(first.getText(),second.getText());
                ui.primaryActionEnabled(generate,copy.ui("creationConfirmGenerate"),R.drawable.ic_lock,format&&same);
                feedback.setText(!characters?getString(R.string.new_password_characters):second.length()>0?getString(same?(format?R.string.new_password_ready:R.string.new_password_incomplete):R.string.new_password_mismatch):format?getString(R.string.new_password_repeat):"");
                feedback.setTextColor(characters&&same?AppUi.ACCENT:AppUi.ERROR);
                feedback.setVisibility(feedback.length()==0?View.GONE:View.VISIBLE);
            };
            android.text.TextWatcher validateChanged=new android.text.TextWatcher(){
                public void beforeTextChanged(CharSequence x,int start,int count,int after){}
                public void onTextChanged(CharSequence x,int start,int before,int count){validate.run();}
                public void afterTextChanged(android.text.Editable x){}
            };
            first.addTextChangedListener(validateChanged);second.addTextChangedListener(validateChanged);validate.run();
            ui.link(p,copy.ui("cancel"),0,this::closeCreationDialog).setTag("password-cancel");
            first.setImeOptions(first.getImeOptions()|android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
            first.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_NEXT){second.requestFocus();return true;}return false;});
            second.setImeOptions(second.getImeOptions()|android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
            second.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){requestWalletGeneration(first,second,note,revision);return true;}return false;});
        },this::closeCreationDialog);bindPasswordDismiss(owner);secure(owner);
    }
    private void closeCreationDialog(){
        closeWeakPasswordConfirm();Dialog owner=creationDialog;creationDialog=null;creationFeedback=null;creationSubmit=null;
        if(owner!=null){hideKeyboard();owner.dismiss();}
    }
    private boolean currentSetup(EditText first,EditText second,EditText note){
        return resumed&&!busy&&!fileBusy&&screen.equals("setup")&&creationDialog!=null&&creationDialog.isShowing()&&first.isAttachedToWindow()&&second.isAttachedToWindow()&&note.isAttachedToWindow();
    }
    private String validateWalletInputs(char[] first,char[] second,EditText note){
        String normalized=WalletNote.normalize(note.getText().toString());WalletCrypto.requireNewPassword(first);
        if(!Arrays.equals(first,second))throw new IllegalArgumentException("两次密码不一致");
        if(enhanced&&(cameraEntropy==null||!cameraEntropy.ready()))throw new IllegalArgumentException("相机增强采集尚未完成");
        return normalized;
    }
    private void requestWalletGeneration(EditText first,EditText second,EditText note,int[] revision){
        if(!allowSecretOperation())return;
        if(weakPasswordConfirm!=null||!currentSetup(first,second,note))return;
        char[] a=chars(first),b=chars(second);boolean weak;
        try{validateWalletInputs(a,b,note);weak=PasswordStrength.assess(java.nio.CharBuffer.wrap(a))==PasswordStrength.Level.WEAK;}
        catch(IllegalArgumentException ex){error(ex.getMessage());return;}
        finally{WalletCrypto.wipe(a);WalletCrypto.wipe(b);}
        if(weak)confirmWeakPassword(first,second,note,revision);else generateWallet(first,second,note);
    }
    private void confirmWeakPassword(EditText first,EditText second,EditText note,int[] revision){
        hideKeyboard();Dialog owner=new Dialog(this);weakPasswordConfirm=owner;
        int token=epoch,approvedRevision=revision[0];boolean enhancedAtOpen=enhanced;CameraEntropy entropyAtOpen=cameraEntropy;
        owner.setOnDismissListener(d->{if(weakPasswordConfirm==owner)weakPasswordConfirm=null;});
        ui.showSheet(owner,getString(R.string.weak_password_title),p->{
            ui.note(p,getString(R.string.weak_password_description));
            ui.callout(p,R.drawable.ic_lock,getString(R.string.weak_password_camera));ui.space(p,24);
            ui.action(p,getString(R.string.weak_password_edit),true,R.drawable.ic_lock,()->{
                if(weakPasswordConfirm!=owner||!owner.isShowing()||!currentSetup(first,second,note)||epoch!=token)return;
                closeWeakPasswordConfirm();first.requestFocus();first.setSelection(first.length());
                first.post(()->{if(resumed&&first.isAttachedToWindow()){
                    InputMethodManager keyboard=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    if(keyboard!=null)keyboard.showSoftInput(first,InputMethodManager.SHOW_IMPLICIT);
                }});
            });
            ui.link(p,getString(R.string.weak_password_continue),0,()->{
                if(weakPasswordConfirm!=owner||!owner.isShowing()||!currentSetup(first,second,note)||epoch!=token)return;
                if(revision[0]!=approvedRevision||enhanced!=enhancedAtOpen||cameraEntropy!=entropyAtOpen){
                    closeWeakPasswordConfirm();error(getString(R.string.weak_password_changed));return;
                }
                closeWeakPasswordConfirm();generateWallet(first,second,note);
            });
        },null,R.drawable.ic_password_warning);
    }
    private void closeWeakPasswordConfirm(){Dialog owner=weakPasswordConfirm;weakPasswordConfirm=null;if(owner!=null)owner.dismiss();}
    private void generateWallet(EditText first,EditText second,EditText note){
        if(!allowSecretOperation())return;
        if(!currentSetup(first,second,note))return;
        char[] a=chars(first),b=chars(second);String normalized;
        try{normalized=validateWalletInputs(a,b,note);}
        catch(IllegalArgumentException ex){WalletCrypto.wipe(a);WalletCrypto.wipe(b);error(ex.getMessage());return;}
        WalletCrypto.wipe(b);setupNote=normalized;CameraEntropy entropy=enhanced?cameraEntropy:null;cameraEntropy=null;
        trackPassword(a);hideKeyboard();int token=begin(creationDialog,creationSubmit,copy.ui("creationConfirmGenerate"),R.drawable.ic_lock);ui.clearPasswords();
        worker.execute(()->{try{WalletCrypto.Card result=crypto.generate(a,WalletCrypto.Network.MAINNET,normalized,entropy);
            runOnUiThread(()->{if(epoch!=token||!resumed||!allowSecretOperation())return;endWork();closeCreationDialog();session=new CreationSession(result.encrypted);copyWriting=false;lockAfterWrite=false;lockStatus="";screen="write";render();});
        }catch(Exception ex){failure(token,ex,"生成失败，未创建冷钱包");}finally{forgetPassword(a);if(entropy!=null)entropy.close();}});
    }
    private void requestCamera(){
        try{setupNote=WalletNote.normalize(setupNote);}catch(Exception ex){error(ex.getMessage());return;}
        if(checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){closeSheet();sheet=ui.sheet(copy.ui("cameraPermission"),p->{ui.note(p,copy.ui("permissionBody"));ui.action(p,copy.ui("allowCamera"),true,0,()->{closeSheet();requestPermissions(new String[]{android.Manifest.permission.CAMERA},CAMERA_PERMISSION);});ui.action(p,copy.ui("notNow"),false,0,this::closeSheet);});}
        else startCamera();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==CAMERA_PERMISSION){if(results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)whenResumed(this::startCamera);else whenResumed(()->cameraFailure(true));}}
    private void startCamera(){clearCamera();ui.clearPasswords();screen="camera";render();}
    private void cameraPage(){
        ui.heading(copy.ui("cameraMode"),copy.ui("cameraTitle"),copy.ui("cameraDesc"));
        FrameLayout frame=new FrameLayout(this);frame.setTag("camera-preview");frame.setBackground(ui.shape(0xff363d37,20,false));frame.setClipToOutline(true);ui.page.addView(frame,new LinearLayout.LayoutParams(-1,ui.dp(300)));
        android.view.TextureView preview=new android.view.TextureView(this);frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        View guide=new View(this);android.graphics.drawable.GradientDrawable outline=ui.shape(Color.TRANSPARENT,10,false);outline.setStroke(ui.borderWidth(),0x70ffffff);guide.setBackground(outline);FrameLayout.LayoutParams gp=new FrameLayout.LayoutParams(-1,-1);gp.setMargins(ui.dp(27),ui.dp(36),ui.dp(27),ui.dp(36));frame.addView(guide,gp);
        TextView live=cameraBadge(copy.ui("livePreview"));FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);lp.topMargin=ui.dp(13);lp.leftMargin=ui.dp(13);frame.addView(live,lp);
        TextView progress=cameraBadge(getString(R.string.camera_progress,"",0).trim());progress.setTag("camera-frame-count");FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);pp.topMargin=ui.dp(13);pp.rightMargin=ui.dp(13);frame.addView(progress,pp);
        TextView hint=new TextView(this);hint.setText(copy.ui("sampling"));hint.setTextSize(11);hint.setTextColor(Color.WHITE);hint.setGravity(Gravity.CENTER);hint.setShadowLayer(ui.dp(3),0,0,0xaa000000);FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);hp.bottomMargin=ui.dp(15);frame.addView(hint,hp);
        LinearLayout privacy=ui.horizontal();privacy.setGravity(Gravity.TOP);privacy.setPadding(0,ui.dp(17),0,ui.dp(10));ui.page.addView(privacy);
        ImageView shield=new ImageView(this);shield.setImageDrawable(ui.drawable(R.drawable.ic_shield,AppUi.ACCENT,17));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ui.dp(17),ui.dp(21));sp.rightMargin=ui.dp(9);privacy.addView(shield,sp);TextView privacyText=ui.text(privacy,copy.ui("captureHint"),11,AppUi.SUB,false);privacyText.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));ui.line(privacyText,1.85f);privacyText.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->ui.alignFirstLine(shield,privacyText));
        Button capture=ui.action(ui.page,copy.ui("capture"),true,R.drawable.ic_camera,()->{if(camera!=null)camera.capture();});ui.primaryActionEnabled(capture,copy.ui("capture"),R.drawable.ic_camera,false);
        ui.link(ui.page,copy.ui("cancelCapture"),0,()->{clearCamera();navigate("setup");});ui.fitArtwork(frame,200);int token=epoch;
        try{camera=new CameraCapture(this,preview,new CameraCapture.Listener(){
            public void progress(int count){progress.setText(getString(R.string.camera_progress,"",count).trim());hint.setText(copy.ui(count==32?"samplingComplete":"sampling"));ui.primaryActionEnabled(capture,copy.ui("capture"),R.drawable.ic_camera,count==32);}
            public void complete(CameraEntropy result,Bitmap photo){
                if(epoch!=token||!resumed||!screen.equals("camera")){result.close();photo.eraseColor(Color.TRANSPARENT);photo.recycle();return;}
                camera=null;cameraEntropy=result;photo.eraseColor(Color.TRANSPARENT);photo.recycle();screen="setup";render();
            }
            public void failed(){if(epoch==token&&resumed){camera=null;cameraFailure();}}
        });}catch(Exception ex){cameraFailure();}
    }
    private TextView cameraBadge(String value){
        TextView badge=new TextView(this);badge.setText(value);badge.setTextColor(Color.WHITE);badge.setTextSize(11);badge.setIncludeFontPadding(false);badge.setBackground(ui.shape(0xab211e19,7,false));badge.setPadding(ui.dp(8),ui.dp(5),ui.dp(8),ui.dp(5));return badge;
    }
    private void cameraReview(){screen="review";render();}
    private void cameraReviewPage(){
        ui.statusIcon(R.drawable.ic_camera);ui.heading(null,copy.ui("cameraReview"),copy.ui("reviewDesc"));
        cameraPhotoView=new ImageView(this);cameraPhotoView.setImageBitmap(cameraPhoto);cameraPhotoView.setScaleType(ImageView.ScaleType.CENTER_CROP);cameraPhotoView.setContentDescription(copy.ui("finalPhoto"));cameraPhotoView.setBackground(ui.shape(0xff363d37,18,false));cameraPhotoView.setClipToOutline(true);ui.page.addView(cameraPhotoView,new LinearLayout.LayoutParams(-1,ui.dp(300)));
        ui.space(ui.page,6);LinearLayout stats=ui.horizontal();ui.page.addView(stats);String[] labels={copy.ui("previewFrames"),copy.ui("finalPhoto")},values={"32 "+copy.ui("frames"),copy.ui("onePhoto")};
        for(int i=0;i<2;i++){LinearLayout box=ui.vertical();box.setPadding(ui.dp(13),ui.dp(13),ui.dp(13),ui.dp(13));box.setBackground(ui.shape(Color.WHITE,14,true));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);if(i==0)bp.rightMargin=ui.dp(10);stats.addView(box,bp);ui.text(box,labels[i],12,AppUi.SUB,false);ui.text(box,values[i],12,AppUi.FG,true);}
        ui.helper(ui.page,copy.ui("cameraNote"));ui.stretch();ui.action(ui.page,copy.ui("useCapture"),true,R.drawable.ic_shuffle,()->{clearCameraPhoto();navigate("setup");});ui.link(ui.page,copy.ui("retake"),0,this::startCamera);
    }
    private void clearCameraPhoto(){if(cameraPhotoView!=null){cameraPhotoView.setImageDrawable(null);cameraPhotoView=null;}if(cameraPhoto!=null){if(!cameraPhoto.isRecycled()){cameraPhoto.eraseColor(Color.TRANSPARENT);cameraPhoto.recycle();}cameraPhoto=null;}}
    private void cameraFailure(){cameraFailure(false);}
    private void cameraFailure(boolean denied){clearCamera();screen="setup";render();closeSheet();sheet=ui.sheet(copy.ui(denied?"deniedTitle":"failedTitle"),p->{ui.note(p,copy.ui(denied?"deniedBody":"failedBody"));ui.action(p,copy.ui("retry"),true,R.drawable.ic_camera,()->{closeSheet();requestCamera();});ui.action(p,copy.ui("standard"),false,0,()->{closeSheet();enhanced=false;render();});ui.link(p,copy.ui("cancel"),0,this::closeSheet);});}
    private void clearCamera(){clearCameraPhoto();if(camera!=null){camera.close();camera=null;}if(cameraEntropy!=null){cameraEntropy.close();cameraEntropy=null;}}
    private void writing(){
        if(!copyWriting)ui.progress(2);
        ui.heading(copyWriting?copy.ui("writeAnother"):copy.ui("writeStep"),copy.ui("writeTitle"),copy.ui(copyWriting?"anotherDesc":"writeDesc"));
        ui.link(ui.page,copy.section("cardHelpCopy").optString("title"),R.drawable.ic_help,this::cardHelp);
        art(true,236);
        noteCard(session.note,true,false);addressCard(ui.page,session.addresses,session.addresses.nativeSegwit,true);
        LinearLayout lockCard=ui.mode(ui.page);ui.toggle(lockCard,copy.section("lockCopy").optString("toggle"),R.drawable.ic_lock,lockAfterWrite,on->lockAfterWrite=on);ui.helper(lockCard,copy.section("lockCopy").optString("sub"));
        ui.stretch();ui.action(ui.page,"开始写卡",true,R.drawable.ic_nfc,()->arm(Mode.CHECK));
        ui.link(ui.page,session.backupSaved()?"再次保存加密备份":copy.ui("backupEarly"),0,()->backupSheet(null));
        if(copyWriting)ui.link(ui.page,"返回完成页",0,()->{copyWriting=false;navigate("done");});

    }
    private void completed(){
        ui.progress(3);ui.successIcon(false);boolean saved=session.backupSaved();
        rememberCreatedWallet(session);
        ui.heading(null,lockAfterWrite&&!"locked".equals(lockStatus)?getString(R.string.nfc_written_not_locked):saved?"冷钱包制作完成":copy.ui("completeTitle"),copy.ui("completeDesc"));
        if(!lockStatus.isEmpty())ui.callout(ui.page,lockStatus.equals("locked")?R.drawable.ic_shield:R.drawable.ic_warning,copy.section("lockCopy").optString(lockStatus));if(candidate!=null&&lockStatus.equals("unknown"))ui.link(ui.page,copy.section("lockCopy").optString("retryVerify"),R.drawable.ic_lock,()->arm(Mode.VERIFY_LOCK));else if(candidate!=null&&lockStatus.equals("unprotected")&&lockAfterWrite)ui.link(ui.page,copy.section("lockCopy").optString("retryLock"),R.drawable.ic_lock,this::confirmReadOnly);noteCard(session.note,true,false);
        addressCard(ui.page,session.addresses,session.addresses.nativeSegwit,true);ui.space(ui.page,18);
        ui.check("生成时恢复校验通过",true);ui.check("NFC 写入后读回一致",true);
        ui.check(saved?"加密备份已保存并核对":"请另外保存一份加密备份",saved);
        if(saved&&session.canFinish())ui.callout(ui.page,R.drawable.ic_record_check,getString(creationRecordSaving?R.string.verification_loading:recordSaveFailed?R.string.verification_save_failed:R.string.verification_save_ok));
        if(session.cardCount()>1)ui.note(ui.page,"此钱包已写入 "+session.cardCount()+" 张 NFC 卡片。");
        ui.stretch();
        ui.action(ui.page,saved?"完成，返回首页":"保存加密备份",true,saved?R.drawable.ic_check:R.drawable.ic_download,saved?this::leaveCreation:()->backupSheet(null));
        ui.action(ui.page,"写入另一张 NFC 卡",false,R.drawable.ic_nfc,()->{if(!session.backupSaved())backupSheet("copy");else anotherCard();});
        if(saved)ui.link(ui.page,"再次保存加密备份",R.drawable.ic_download,()->backupSheet(null));
        ui.foot("首次充值前，建议再读取并解密验证一次。");
    }
    private void anotherCard(){lockAfterWrite=false;lockStatus="";copyWriting=true;navigate("write");}
    private void reading(){
        screen="read";ui.heading(null,copy.ui("readTitle"),copy.ui("readDesc"));
        art(true,236);ui.foot(copy.ui("nfcHint"));ui.stretch();
        ui.action(ui.page,"读取 NFC 卡片",true,R.drawable.ic_nfc,()->arm(Mode.READ));
        ui.action(ui.page,"导入加密备份文件",false,R.drawable.ic_download,this::openBackup);
        ui.foot("仅在本机处理，无需联网");
    }
    private void unlocking(){
        boolean bip38=incoming.startsWith("6P");WalletCrypto.Metadata meta=bip38?null:crypto.inspect(incoming);
        boolean verified=currentVerification()!=null;
        if(previousVerification!=null&&!verified){
            ui.statusIcon(R.drawable.ic_shield);
            ui.heading(getString(R.string.verification_changed),getString(R.string.verification_changed_title),getString(R.string.verification_changed_body));
        }else recoveryHeading(false,verified?R.string.verification_matched_intro:R.string.verification_pending_body);
        if(verified){
            recoveryNote(publicVerification.note,false);addressCard(ui.page,publicVerification.addresses,publicVerification.addresses.nativeSegwit,true);
        }else if(meta!=null){
            recoveryNote(meta.note,meta.version<3);addressCard(ui.page,meta.addresses,meta.address,false);
            if(meta.addresses==null)ui.note(ui.page,"这是旧版 CG1 备份，其他地址将在解密后显示。");
        }else{recoveryNote("",true);ui.note(ui.page,"BIP38 密文不包含公开收款地址，解密后请核对实际存币地址。");}
        if(recordReadFailed)recoveryNotice(R.drawable.ic_warning,getString(R.string.verification_load_failed));
        if(recordSaveFailed)recoveryNotice(R.drawable.ic_warning,getString(R.string.verification_save_failed));
        if(previousVerification!=null&&!verified)recoveryNotice(R.drawable.ic_warning,getString(R.string.verification_changed_warning));
        if(verified)recoveryNotice(R.drawable.ic_shield,getString(verificationSource.equals("local")?R.string.verification_local_body:R.string.verification_locked_body));
        recoveryActionsGap();Button unlock=recoveryAction(getString(verified?R.string.wallet_view_private:R.string.wallet_unlock_action),true,verified?R.drawable.ic_display:R.drawable.ic_unlock,()->showUnlockDialog(verified));
        if(recordLoading)ui.loadingAction(unlock,getString(R.string.verification_loading));
        recoveryAction("换一张卡片 / 备份",false,R.drawable.ic_shuffle,this::switchWallet);recoveryFooter("仅在本机处理，无需联网");
    }
    private void switchWallet(){
        closeSheet();sheet=ui.sheet("换一张卡片 / 备份",p->{
            ui.note(p,getString(R.string.wallet_switch_description));
            ui.action(p,"读取 NFC 卡片",true,R.drawable.ic_nfc,()->{incoming=null;navigate("read");arm(Mode.READ);});
            ui.action(p,"导入加密备份文件",false,R.drawable.ic_download,()->{incoming=null;navigate("read");openBackup();});
        });
    }
    private void recoveryHeading(boolean recovered){
        recoveryHeading(recovered,recovered?R.string.verification_session_body:R.string.wallet_read_description);
    }
    private boolean compactRecovery(){return getResources().getConfiguration().screenHeightDp<720;}
    private void recoveryHeading(boolean recovered,int descriptionId){
        ui.statusIcon(recovered?R.drawable.ic_unlock:R.drawable.ic_lock);
        View badge=ui.page.getChildAt(ui.page.getChildCount()-1);
        LinearLayout.LayoutParams badgeParams=(LinearLayout.LayoutParams)badge.getLayoutParams();badgeParams.topMargin=ui.dp(compactRecovery()?0:4);badgeParams.bottomMargin=ui.dp(compactRecovery()?6:12);
        ui.heading(copy.ui(recovered?"decrypted":"recognized"),copy.ui(recovered?"revealedTitle":"unlockTitle"),null);
        TextView description=ui.text(ui.page,getString(descriptionId),12,AppUi.SUB,false);
        ui.line(description,1.75f);ui.space(ui.page,compactRecovery()?4:6);
    }
    private void recoveryNote(String note,boolean legacy){
        LinearLayout card=ui.card(ui.page),head=ui.horizontal();card.setMinimumHeight(ui.dp(compactRecovery()?64:72));card.addView(head);
        compactRecoveryCard(card);
        TextView label=ui.text(head,copy.ui("publicRemark"),11,AppUi.SUB,false);label.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        ui.text(head,getString(R.string.wallet_note_public),11,AppUi.SUB,false).setLayoutParams(new LinearLayout.LayoutParams(-2,-2));
        ui.space(card,6);TextView value=ui.text(card,"",18,AppUi.FG,true);value.setText(note.isEmpty()?copy.ui(legacy?"oldCard":"noRemark"):note);
    }
    private void compactRecoveryCard(LinearLayout card){
        card.setPadding(ui.dp(12),ui.dp(compactRecovery()?8:12),ui.dp(12),ui.dp(compactRecovery()?6:8));
        LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)card.getLayoutParams();params.topMargin=0;params.bottomMargin=ui.dp(compactRecovery()?6:8);card.setLayoutParams(params);
    }
    private void recoveryNotice(int icon,String message){
        LinearLayout body=ui.callout(ui.page,icon,message),panel=(LinearLayout)body.getParent();int padding=ui.dp(compactRecovery()?8:10);panel.setPadding(ui.dp(12),padding,ui.dp(12),padding);
        ((LinearLayout.LayoutParams)panel.getLayoutParams()).topMargin=ui.dp(compactRecovery()?6:8);
    }
    private void recoveryActionsGap(){ui.page.addView(new View(this),new LinearLayout.LayoutParams(1,ui.dp(compactRecovery()?4:8),1));}
    private Button recoveryAction(String label,boolean primary,int icon,Runnable action){
        Button button=ui.action(ui.page,label,primary,icon,action);((LinearLayout.LayoutParams)button.getLayoutParams()).topMargin=ui.dp(compactRecovery()?6:8);return button;
    }
    private void recoveryFooter(String message){ui.foot(message);ui.page.getChildAt(ui.page.getChildCount()-1).setPadding(0,ui.dp(compactRecovery()?4:6),0,0);}
    private void showUnlockDialog(){
        showUnlockDialog(false);
    }
    private void showUnlockDialog(boolean viewPrivate){
        if(!allowSecretOperation())return;
        if(!resumed||busy||fileBusy||incoming==null||!screen.equals("unlock")||unlockDialog!=null)return;
        closeSheet();requestedPrivate=viewPrivate;Dialog owner=new Dialog(this);unlockDialog=owner;
        owner.setOnDismissListener(d->{
            if(busy&&workDialog==owner)cancelWork();
            if(owner.getWindow()!=null)ui.releaseInputs(owner.getWindow().getDecorView());
            if(unlockDialog==owner){unlockDialog=null;unlockFeedback=null;unlockSubmit=null;}
        });
        ui.showSheet(owner,getString(viewPrivate?R.string.verification_unlock_private:R.string.wallet_unlock_action),p->{
            ui.note(p,getString(R.string.verification_unlock_body));
            EditText pass=ui.input(p,"钱包解密密码",true,null);
            ui.helper(p,getString(R.string.wallet_unlock_local));
            ui.callout(p,R.drawable.ic_shield,getString(R.string.verification_password_risk));
            unlockFeedback=ui.text(p,"",12,AppUi.ERROR,false);unlockFeedback.setVisibility(View.GONE);unlockFeedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            ui.space(p,13);unlockSubmit=ui.action(p,getString(R.string.wallet_unlock_action),true,R.drawable.ic_unlock,()->unlockFromDialog(owner,pass));
            ui.link(p,"取消",0,this::closeUnlockDialog).setTag("password-cancel");
            pass.setImeOptions(pass.getImeOptions()|android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
            pass.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){unlockFromDialog(owner,pass);return true;}return false;});
        },this::closeUnlockDialog);bindPasswordDismiss(owner);secure(owner);
    }
    private void unlockFromDialog(Dialog owner,EditText pass){
        if(!allowSecretOperation())return;
        if(unlockDialog!=owner||!owner.isShowing()||!resumed||busy||fileBusy||incoming==null||!screen.equals("unlock"))return;
        char[] password=chars(pass);
        if(password.length==0){WalletCrypto.wipe(password);unlockFeedback.setText(tr("请输入解密密码"));unlockFeedback.setVisibility(View.VISIBLE);return;}
        // Old backups can have shorter passwords; the ten-character minimum applies only to creation.
        trackPassword(password);hideKeyboard();int token=begin(owner,unlockSubmit,getString(R.string.wallet_unlock_action),R.drawable.ic_unlock);ui.clearPasswords();String encrypted=incoming;boolean bip38=encrypted.startsWith("6P");boolean showPrivate=requestedPrivate;String showAddress=pendingAddressType,cardHint=incomingCardId;
        worker.execute(()->{
            try{
                PrivateSession result=PrivateSession.seal(bip38?crypto.importBip38(encrypted,password):crypto.decrypt(encrypted,password));
                VerificationStore.Record record;
                try{record=VerificationStore.Record.verified(encrypted,result.addresses,result.note,cardHint==null?java.util.Collections.emptyList():java.util.Collections.singleton(cardHint));}catch(Exception ex){result.close();throw ex;}
                runOnUiThread(()->{
                    if(epoch!=token||!resumed||!encrypted.equals(incoming)||!allowSecretOperation()){result.close();return;}
                    endWork();closeUnlockDialog();closeUnlocked();unlocked=result;unlockDeadline.start(android.os.SystemClock.elapsedRealtime());tickUnlockedSession();haptic(true);
                    if(previousVerification!=null&&publicVerification==null){pendingVerification=record;confirmChangedRecord(result,record,showPrivate,showAddress);}
                    else finishVerifiedUnlock(record,showPrivate,showAddress);
                });
            }catch(Exception ex){runOnUiThread(()->{
                if(epoch!=token||!resumed)return;endWork();showUnlockDialog();
                if(unlockFeedback!=null){unlockFeedback.setText(getString(R.string.verification_failed));unlockFeedback.setVisibility(View.VISIBLE);}haptic(false);
            });}finally{forgetPassword(password);}
        });
    }
    private void closeUnlockDialog(){
        Dialog owner=unlockDialog;unlockDialog=null;unlockFeedback=null;unlockSubmit=null;pendingAddressType=null;requestedPrivate=false;if(owner!=null)owner.dismiss();
    }
    private void revealed(){
        recoveryHeading(true);
        recoveryNote(unlocked.note,!incoming.startsWith(WalletCrypto.NOTE_PREFIX));addressCard(ui.page,unlocked.addresses,unlocked.address,true);
        recoveryNotice(R.drawable.ic_lock,getString(R.string.verification_session_limit));if(recordSaveFailed)recoveryNotice(R.drawable.ic_warning,getString(R.string.verification_save_failed));
        recoveryActionsGap();recoveryAction(getString(R.string.wallet_view_private),true,R.drawable.ic_display,this::showPrivateQr);
        recoveryAction("隐藏私钥，返回首页",false,R.drawable.ic_lock,()->{incoming=null;navigate("home");});
        recoveryFooter("私钥和二维码可控制全部 BTC，请勿分享。");
    }
    private void art(boolean scanning,int height){ui.page.addView(new WalletArtView(this,scanning),new LinearLayout.LayoutParams(-1,ui.dp(height)));}
    private void addressCard(LinearLayout parent,BitcoinAddresses addresses,String nativeAddress,boolean verified){
        boolean trusted=receivingAllowed(nativeAddress,verified);
        LinearLayout card=ui.card(parent),top=ui.horizontal();card.addView(top);
        boolean recovery=screen.equals("unlock")||screen.equals("revealed");
        if(recovery)compactRecoveryCard(card);
        TextView label=new TextView(this);label.setIncludeFontPadding(false);label.setText(copy.ui("receive")+(recovery?" · SegWit":""));label.setTextSize(11);label.setTextColor(AppUi.SUB);top.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=new TextView(this);badge.setIncludeFontPadding(false);badge.setText(recovery?addressVerificationLabel(trusted):trusted?"SegWit":getString(R.string.wallet_address_pending));badge.setTextSize(11);badge.setTextColor(AppUi.ACCENT);top.addView(badge);
        TextView value=ui.text(card,trusted?nativeAddress:shortAddress(nativeAddress),13,AppUi.FG,false);value.setTypeface(Typeface.MONOSPACE);value.setSingleLine(false);value.setHorizontallyScrolling(false);value.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);value.setPadding(0,ui.dp(recovery?10:16),0,ui.dp(recovery?6:8));
        LinearLayout links=ui.horizontal();card.addView(links);
        if(addresses!=null){LinearLayout otherSlot=ui.horizontal();links.addView(otherSlot,new LinearLayout.LayoutParams(0,-2,1));ui.textAction(otherSlot,copy.ui("other"),0,R.drawable.ic_chevron_down,()->otherAddresses(addresses,trusted));}
        else{TextView network=new TextView(this);network.setText(R.string.btc_mainnet);network.setTextSize(11);network.setTextColor(AppUi.SUB);links.addView(network,new LinearLayout.LayoutParams(0,-2,1));}
        links.addView(ui.inline(trusted?tr("展示地址"):getString(R.string.verification_after_check),trusted?R.drawable.ic_display:R.drawable.ic_lock,()->showAddressQr("SegWit",nativeAddress,trusted)));
    }
    private void otherAddresses(BitcoinAddresses a,boolean verified){
        closeSheet();sheet=ui.sheet("其他收款地址",p->{
            addressRow(p,"SegWit",a.nativeSegwit,verified,true);addressRow(p,"Legacy",a.legacy,verified,true);addressRow(p,"P2SH",a.nestedSegwit,verified,true);
            addressRow(p,"Taproot",a.taproot,verified,false);
        });
    }
    private void addressRow(LinearLayout p,String title,String address,boolean verified,boolean border){
        boolean trusted=receivingAllowed(address,verified);
        ui.space(p,12);LinearLayout heading=ui.horizontal();p.addView(heading);
        TextView name=ui.text(heading,title,13,AppUi.FG,true);name.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        TextView status=ui.text(heading,addressVerificationLabel(trusted),11,AppUi.ACCENT,false);status.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));
        TextView value=ui.text(p,trusted?address:shortAddress(address),12,AppUi.SUB,false);value.setTypeface(Typeface.MONOSPACE);value.setPadding(0,ui.dp(8),0,0);
        LinearLayout links=ui.horizontal();p.addView(links);
        Button show=ui.inline(trusted?tr("展示地址"):getString(R.string.verification_after_check),trusted?R.drawable.ic_display:R.drawable.ic_lock,()->showAddressQr(title,address,trusted));show.setContentDescription((trusted?tr("展示地址"):getString(R.string.verification_after_check))+" "+title);
        links.addView(show);
        if(border)ui.divider(p);
    }
    private void transferHelp(){dismissQr();closeSheet();sheet=helpPages.transfer(()->{closeSheet();if(resumed&&unlocked!=null)showPrivateQr();});}
    private void help(){closeSheet();sheet=helpPages.about(this::closeSheet,()->openArticle("security"),screen.equals("home")?this::language:null,()->openArticle("verification"));}
    private void openArticle(String article){articleReturn=screen;articleScroll=ui.scroll.getScrollY();closeSheet();if(screen.equals("revealed")){dismissQr();closeUnlocked();articleReturn="unlock";}screen=article;render();}
    private void returnFromSecurity(){String target=articleReturn.equals("revealed")?"unlock":articleReturn;navigate(target);ui.scroll.post(()->ui.scroll.scrollTo(0,articleScroll));help();}
    private boolean allowSecretOperation(){
        if(accessGate!=null&&!accessGate.blocked())return true;
        stopAccessibleSecrets();showAccessNotice();return false;
    }
    private void accessibilityChanged(){
        if(accessGate==null||!accessGate.blocked())return;
        boolean sensitive=unlocked!=null||privateOpening||unlockDialog!=null||creationDialog!=null||busy||camera!=null||cameraEntropy!=null||pendingQrPrivate||filePrivate||(clipboardMarker!=null&&clipboardExpiry>android.os.SystemClock.elapsedRealtime());
        if(sensitive){stopAccessibleSecrets();showAccessNotice();}
    }
    private void stopAccessibleSecrets(){
        epoch++;dismissWork();ui.clearPasswords();wipePendingPasswords();closeCreationDialog();closeUnlockDialog();
        if(filePrivate)cancelDocumentOperation(true);
        if(pendingQrPrivate)clearPendingQrImage();
        boolean hadPrivate=unlocked!=null||privateOpening;
        if(hadPrivate)dismissQr();closeUnlocked();clearCamera();
        clipboardExpiry=0;expirePrivateClipboard();
        if(screen.equals("revealed")){screen="unlock";render();}
        else if(screen.equals("camera")||screen.equals("review")){screen="setup";render();}
    }
    private void showAccessNotice(){
        if(isFinishing()||isDestroyed()||accessNotice!=null)return;
        if(!resumed){afterResume=this::showAccessNotice;return;}
        Dialog owner=new Dialog(this);accessNotice=owner;
        ui.statusSheet(owner,getString(R.string.access_guard_title),R.drawable.ic_shield,p->{
            TextView body=ui.text(p,getString(R.string.access_guard_body),15,AppUi.SUB,false);ui.line(body,1.8f);
            ui.callout(p,R.drawable.ic_lock,getString(R.string.access_guard_detail));ui.space(p,12);
            ui.action(p,getString(R.string.access_guard_ok),true,0,this::closeAccessNotice);
        });
        owner.setOnDismissListener(d->{if(accessNotice==owner)accessNotice=null;});secure(owner);
    }
    private void closeAccessNotice(){Dialog owner=accessNotice;accessNotice=null;if(owner!=null)owner.dismiss();}
    private void onObscuredTouch(){
        if(!resumed||isFinishing()||isDestroyed()||rendering)return;
        if(busy||fileBusy||nfcBusy){toast(getString(R.string.obscured_retry));return;}
        if(screen.equals("protection"))return;
        protectionReturn=screen.equals("revealed")?"unlock":screen;
        if(screen.equals("camera")||screen.equals("review")){
            // Invalidate camera callbacks before closing capture; never sample behind this notice.
            epoch++;clearCamera();protectionReturn="setup";
        }
        // No protected action ran. Do not retain a displayed WIF, QR or an old export authorization.
        clearPendingQrImage();wipePendingPasswords();
        navigate("protection");
    }
    private void protectionPage(){
        ui.statusIcon(R.drawable.ic_shield);
        ui.heading(getString(R.string.obscured_kicker),getString(R.string.obscured_title),getString(R.string.obscured_description));
        LinearLayout card=ui.card(ui.page);
        protectionAdvice(card,R.drawable.ic_eye_open,getString(R.string.obscured_remove));
        ui.divider(card);protectionAdvice(card,R.drawable.ic_check,getString(R.string.obscured_retry));
        ui.callout(ui.page,R.drawable.ic_shield,getString(R.string.obscured_boundary));
        ui.stretch();ui.action(ui.page,getString(R.string.obscured_return),true,0,this::returnFromProtection);
    }
    private void protectionAdvice(LinearLayout parent,int symbol,String message){
        LinearLayout row=ui.horizontal();row.setGravity(Gravity.TOP);row.setPadding(0,ui.dp(13),0,ui.dp(13));parent.addView(row);
        ImageView image=new ImageView(this);image.setImageDrawable(ui.drawable(symbol,AppUi.ACCENT,17));
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(ui.dp(18),ui.dp(23));ip.rightMargin=ui.dp(10);row.addView(image,ip);
        TextView text=ui.text(row,message,12,AppUi.SUB,false);text.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));text.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->ui.alignFirstLine(image,text));
    }
    private void returnFromProtection(){String next=protectionReturn;protectionReturn="home";navigate(next);}
    private void cardHelp(){closeSheet();sheet=helpPages.cardHelp(this::closeSheet);}
    private void entryDisclaimer(String next){if(!"setup".equals(next)&&!"read".equals(next))return;entryTarget=next;closeSheet();sheet=helpPages.disclaimer(()->{closeSheet();if("setup".equals(next)){lockAfterWrite=false;lockStatus="";setupNote="";}if("read".equals(next))incoming=null;navigate(next);},()->{closeSheet();screen="legal";render();},this::closeSheet);}
    private void language(){
        if(!screen.equals("home"))return;closeSheet();sheet=ui.sheet(copy.ui("language"),p->{String[] codes={"system","zh","en","ja","ko"},names={copy.ui("system"),"中文","English","日本語","한국어"};String current=Texts.language(this);ui.note(p,copy.ui("current")+"："+languageName(current));
            for(int i=0;i<codes.length;i++){String code=codes[i];boolean selected=code.equals(Texts.selected(this));ui.option(p,names[i],code.equals("system")?languageName(android.content.res.Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage()):null,selected,selected?R.drawable.ic_check_mark:0,()->{closeSheet();Texts.select(this,code);reopenLanguage=true;recreate();});}
            ui.helper(p,copy.ui("systemHint"));ui.link(p,copy.ui("done"),0,this::closeSheet);
        });
    }
    private String languageName(String code){return code.equals("zh")?"中文":code.equals("en")?"English":code.equals("ja")?"日本語":code.equals("ko")?"한국어":java.util.Locale.forLanguageTag(code).getDisplayName(java.util.Locale.forLanguageTag(code));}
    private void noteCard(String note,boolean verified,boolean legacy){
        LinearLayout card=ui.card(ui.page),head=ui.horizontal();card.addView(head);TextView label=ui.text(head,copy.ui("publicRemark"),11,AppUi.SUB,false);label.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));ui.text(head,copy.ui(legacy?"oldCard":verified?"verifiedRemark":"unverified"),11,AppUi.ACCENT,false).setLayoutParams(new LinearLayout.LayoutParams(-2,-2));ui.space(card,9);TextView value=ui.text(card,"",18,AppUi.FG,true);value.setText(note.isEmpty()?copy.ui("noRemark"):note);
        if(!verified&&!legacy)ui.helper(ui.page,copy.ui("remarkWarning"));
    }
    private void explorers(String type,String address,boolean verified){
        if(!receivingAllowed(address,verified))return;
        try{BitcoinAddresses.requireMainnetAddress(address);}catch(Exception ex){error("收款地址不正确，无法查询");return;}
        dismissQr();closeSheet();sheet=ui.sheet(copy.ui("explorerTitle"),p->{
            ui.note(p,copy.ui("explorerDesc"));LinearLayout card=ui.card(p),head=ui.horizontal();card.addView(head);TextView label=ui.text(head,type,11,AppUi.SUB,false);label.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));ui.text(head,copy.ui("mainnet"),11,AppUi.ACCENT,false).setLayoutParams(new LinearLayout.LayoutParams(-2,-2));TextView text=ui.text(card,address,12,AppUi.FG,false);text.setTypeface(Typeface.MONOSPACE);
            ui.callout(p,R.drawable.ic_globe,copy.ui("explorerPrivacy"));ui.helper(p,copy.ui("explorerAction"));
            String[] names={"mempool","Blockstream","Blockchair"},domains={"mempool.space","blockstream.info","blockchair.com"};for(int i=0;i<names.length;i++){int site=i;ui.option(p,names[i],domains[i],false,R.drawable.ic_external,()->openExplorer(address,site));}
            ui.helper(p,copy.ui("explorerSafety"));ui.helper(p,copy.ui("explorerOffline"));ui.link(p,copy.ui("back"),R.drawable.ic_back,()->{closeSheet();showAddressQr(type,address,verified);});
        });
    }
    private void openExplorer(String address,int site){
        if(!receivingAllowed(address,true))return;
        try{
            BitcoinAddresses.requireMainnetAddress(address);if(site<0||site>2)throw new IllegalArgumentException();
            String[] hosts={"mempool.space","blockstream.info","blockchair.com"};
            android.net.Uri uri=new android.net.Uri.Builder().scheme("https").authority(hosts[site]).appendPath(site==2?"bitcoin":"address").build();
            if(site==2)uri=uri.buildUpon().appendPath("address").build();uri=uri.buildUpon().appendPath(address).build();
            // Restrict the recipient to a normal browser handler, never a card-supplied URL or wallet deep link.
            Intent generic=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://example.org/")).addCategory(Intent.CATEGORY_BROWSABLE);
            java.util.List<android.content.pm.ResolveInfo> browsers=getPackageManager().queryIntentActivities(generic,android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            if(browsers.isEmpty())throw new android.content.ActivityNotFoundException();
            Intent open=new Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE);open.setPackage(browsers.get(0).activityInfo.packageName);
            closeSheet();dismissQr();closeUnlocked();ui.clearPasswords();wipePendingPasswords();clearPendingQrImage();clearOwnedClipboard();
            if(screen.equals("revealed"))screen="unlock";startActivity(open);
        }catch(Exception ex){error("无法打开浏览器，请在另一台设备查询公开地址");}
    }
    private void confirmReadOnly(){
        closeSheet();org.json.JSONObject c=copy.section("lockCopy");sheet=ui.sheet(c.optString("title"),p->{ui.note(p,c.optString("desc"));ui.callout(p,R.drawable.ic_lock,c.optString("risk"));ui.action(p,c.optString("confirm"),true,R.drawable.ic_lock,()->{closeSheet();lockStatus="unknown";arm(Mode.LOCK);});ui.link(p,c.optString("skip"),0,()->{lockStatus="unprotected";closeSheet();backupSheet(null);});});
    }
    private void acceptIncoming(String text){acceptIncoming(text,null);}
    private void acceptIncoming(String text,String cardId){
        try{
            if(text.length()>WalletCrypto.MAX_TEXT_LENGTH)throw new IllegalArgumentException();
            if(WalletCrypto.isBackup(text))crypto.inspect(text);
            else if(!(text.length()==58&&text.startsWith("6P")&&text.matches("[1-9A-HJ-NP-Za-km-z]+")))throw new IllegalArgumentException();
            clearIncomingVerification();incoming=text;incomingCardId=cardId!=null&&cardId.matches("(?:[0-9a-f]{2}){1,32}")?cardId:null;navigate("unlock");
        }catch(IllegalArgumentException ex){error("无法读取：备份格式不正确或不受支持");}
    }
    private void clearIncomingVerification(){
        publicVerification=null;previousVerification=null;pendingVerification=null;verificationSource="";
        recordLoading=false;recordReadFailed=false;recordSaveFailed=false;pendingAddressType=null;requestedPrivate=false;
    }
    private VerificationStore.Record currentVerification(){
        return incoming!=null&&publicVerification!=null&&publicVerification.matches(incoming)?publicVerification:null;
    }
    private String shortAddress(String address){return address==null?"":address.length()>20?address.substring(0,8)+" ··· "+address.substring(address.length()-6):address;}
    private boolean hasAddress(BitcoinAddresses a,String value){return a!=null&&value!=null&&(a.nativeSegwit.equals(value)||a.legacy.equals(value)||a.nestedSegwit.equals(value)||a.taproot.equals(value));}
    private boolean receivingAllowed(String address,boolean claimedVerified){
        if(!claimedVerified)return false;
        if((screen.equals("write")||screen.equals("done"))&&session!=null)return session.canFinish()&&hasAddress(session.addresses,address);
        VerificationStore.Record record=currentVerification();
        if(record!=null)return record.containsAddress(address);
        return screen.equals("revealed")&&unlocked!=null&&pendingVerification==null&&hasAddress(unlocked.addresses,address)&&unlockDeadline.cause(android.os.SystemClock.elapsedRealtime())==UnlockDeadline.Cause.NONE;
    }
    private String addressVerificationLabel(boolean verified){
        return getString(verified?verificationSource.equals("local")?R.string.verification_local:R.string.verification_fresh:previousVerification!=null?R.string.verification_changed:R.string.wallet_address_pending);
    }
    private void checkIncomingRecord(){
        if(incoming==null||recordLoading||!screen.equals("unlock"))return;
        final String payload=incoming,cardId=incomingCardId;final int token=epoch;recordLoading=true;render();
        worker.execute(()->{
            VerificationStore.Lookup result=null;boolean failed=false;
            try{result=verificationStore.lookup(payload,cardId);}catch(Exception ex){failed=true;}
            VerificationStore.Lookup lookup=result;boolean unavailable=failed;
            runOnUiThread(()->{
                if(epoch!=token||!resumed||!payload.equals(incoming)||!screen.equals("unlock"))return;
                recordLoading=false;recordReadFailed=unavailable;
                publicVerification=lookup==null?null:lookup.match;previousVerification=lookup==null?null:lookup.previous;
                verificationSource=publicVerification==null?"":"local";render();
            });
        });
    }
    private void persistVerification(VerificationStore.Record record){
        int token=epoch;worker.execute(()->{
            boolean saved=true;try{verificationStore.save(record);}catch(Exception ex){saved=false;}
            boolean failed=!saved;runOnUiThread(()->{if(epoch==token&&resumed&&publicVerification==record){recordSaveFailed=failed;if(failed){render();toast(getString(R.string.verification_save_failed));}}});
        });
    }
    private void finishVerifiedUnlock(VerificationStore.Record record,boolean privateRequested,String addressType){
        if(unlocked==null||expireUnlockedIfNeeded())return;
        publicVerification=record;previousVerification=null;pendingVerification=null;verificationSource="fresh";recordSaveFailed=false;
        screen="revealed";render();persistVerification(record);
        if(privateRequested)showPrivateQr();else if(addressType!=null)showAddressQr(addressType,addressFor(record.addresses,addressType),true);
    }
    private String addressFor(BitcoinAddresses addresses,String type){
        return type.equals("Legacy")?addresses.legacy:type.equals("P2SH")?addresses.nestedSegwit:type.equals("Taproot")?addresses.taproot:addresses.nativeSegwit;
    }
    private void confirmChangedRecord(PrivateSession sessionOwner,VerificationStore.Record record,boolean privateRequested,String addressType){
        // The sealed session still expires while this confirmation is open. No WIF is materialized here.
        Dialog dialog=new Dialog(this);sheet=dialog;int token=epoch;
        ui.showSheet(dialog,getString(R.string.verification_review_title),p->{
            ui.note(p,getString(R.string.verification_review_body));
            VerificationStore.Record before=previousVerification;
            if(before!=null){ui.text(p,getString(R.string.verification_previous),13,AppUi.FG,true);recordSummary(p,before);}
            ui.text(p,getString(R.string.verification_current),13,AppUi.FG,true);recordSummary(p,record);
            ui.action(p,getString(R.string.verification_accept),true,R.drawable.ic_record_check,()->{
                if(epoch!=token||!resumed||sheet!=dialog||!dialog.isShowing()||unlocked!=sessionOwner||pendingVerification!=record||expireUnlockedIfNeeded())return;
                pendingVerification=null;sheet=null;dialog.dismiss();finishVerifiedUnlock(record,privateRequested,addressType);
            });
            ui.action(p,copy.ui("cancel"),false,0,()->{if(sheet==dialog){sheet=null;dialog.dismiss();closeUnlocked();incoming=null;navigate("read");}});
        });
        dialog.setOnDismissListener(d->{if(pendingVerification==record){pendingVerification=null;closeUnlocked();}});secure(dialog);
    }
    private void recordSummary(LinearLayout parent,VerificationStore.Record record){
        LinearLayout card=ui.card(parent);TextView note=ui.text(card,"",14,AppUi.FG,true);note.setText(record.note.isEmpty()?copy.ui("noRemark"):record.note);
        for(String type:new String[]{"SegWit","Legacy","P2SH","Taproot"}){ui.text(card,type,11,AppUi.SUB,false);TextView value=ui.text(card,addressFor(record.addresses,type),12,AppUi.SUB,false);value.setTypeface(Typeface.MONOSPACE);}
    }
    private boolean creationRecordSaving;
    private void rememberCreatedWallet(CreationSession owner){
        if(owner==null||!owner.canFinish()||(recordedCreation==owner&&recordedCreationCards==owner.cardCount()))return;
        recordedCreation=owner;recordedCreationCards=owner.cardCount();int count=recordedCreationCards;String[] cards=owner.writtenCardIds();creationRecordSaving=true;recordSaveFailed=false;int token=epoch;
        worker.execute(()->{
            boolean saved=true;
            try{verificationStore.save(VerificationStore.Record.verified(owner.encrypted,owner.addresses,owner.note,Arrays.asList(cards)));}catch(Exception ex){saved=false;}
            boolean failed=!saved;runOnUiThread(()->{if(session==owner&&recordedCreationCards==count){creationRecordSaving=false;recordSaveFailed=failed;if(epoch==token&&resumed&&screen.equals("done"))render();}});
        });
    }
    private void showRecords(){
        recordRows=java.util.Collections.emptyList();navigate("records");loadRecords();
    }
    private void loadRecords(){
        int token=epoch;worker.execute(()->{
            java.util.List<VerificationStore.Record> rows=null;try{rows=verificationStore.records();}catch(Exception ignored){}
            java.util.List<VerificationStore.Record> result=rows;runOnUiThread(()->{if(epoch==token&&resumed&&screen.equals("records")){recordReadFailed=result==null;recordRows=result==null?java.util.Collections.emptyList():result;render();}});
        });
    }
    private void recordsPage(){
        ui.statusIcon(R.drawable.ic_record_check);
        ui.heading(null,getString(R.string.verification_records_title),getString(R.string.verification_records_body));
        if(recordReadFailed)ui.callout(ui.page,R.drawable.ic_warning,getString(R.string.verification_load_failed));
        if(recordRows.isEmpty()&&!recordReadFailed)ui.note(ui.page,getString(R.string.verification_empty));
        for(VerificationStore.Record record:recordRows){
            LinearLayout card=ui.card(ui.page);TextView note=ui.text(card,"",16,AppUi.FG,true);note.setText(record.note.isEmpty()?copy.ui("noRemark"):record.note);
            ui.text(card,getString(R.string.btc_mainnet),11,AppUi.SUB,false);ui.text(card,shortAddress(record.addresses.nativeSegwit),12,AppUi.SUB,false);
            ui.link(card,getString(R.string.verification_view_public),R.drawable.ic_display,()->{closeSheet();sheet=ui.sheet(getString(R.string.verification_view_public),p->{recordSummary(p,record);ui.action(p,copy.ui("understand"),true,0,this::closeSheet);});});
            ui.link(card,getString(R.string.verification_clear_one),R.drawable.ic_trash,()->confirmClearRecord(record.id));
        }
        ui.stretch();if(!recordRows.isEmpty()||recordReadFailed)ui.action(ui.page,getString(R.string.verification_clear_all),false,R.drawable.ic_trash,()->confirmClearRecord(null));
        ui.action(ui.page,getString(R.string.verification_back_info),true,R.drawable.ic_back,()->navigate("verification"));
    }
    private void confirmClearRecord(String id){
        closeSheet();sheet=ui.sheet(getString(R.string.verification_clear_title),p->{
            ui.note(p,getString(R.string.verification_records_body));
            ui.action(p,getString(R.string.verification_clear_action),true,R.drawable.ic_trash,()->{
                closeSheet();closeUnlocked();dismissQr();clearIncomingVerification();recordRows=java.util.Collections.emptyList();int token=epoch;busy=true;
                worker.execute(()->{boolean ok=true;try{if(id==null)verificationStore.clear();else verificationStore.remove(id);}catch(Exception ex){ok=false;}boolean success=ok;
                    runOnUiThread(()->{if(epoch==token&&resumed){busy=false;toast(getString(success?R.string.verification_cleared:R.string.verification_records_failed));loadRecords();}});
                });
            });
            ui.action(p,getString(R.string.verification_keep),false,0,this::closeSheet);
        });
    }
    private void backupSheet(String then){
        if(session==null)return;closeSheet();
        String title="copy".equals(then)?"先保存备份，再继续写卡":"home".equals(then)?"返回前，请先保存备份":copy.ui("backupTitle");
        sheet=ui.sheet(title,p->{
            ui.note(p,copy.ui("backupBody"));
            if("copy".equals(then))ui.note(p,"保存完成后，将继续把同一个冷钱包写入另一张 NFC 卡。");
            ui.action(p,copy.ui("saveFile"),true,R.drawable.ic_download,()->{closeSheet();saveBackupFile(then);});
            ui.helper(p,"文件保存完成后，会自动读回核对。");ui.link(p,copy.ui("cancel"),0,this::closeSheet);
        });
    }
    private void saveBackupFile(String then){
        if(session==null)return;exportData=session.encrypted;exportAction=then;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.setType("text/plain");intent.addCategory(Intent.CATEGORY_OPENABLE);
        String address=session.addresses.nativeSegwit;intent.putExtra(Intent.EXTRA_TITLE,"bittap-mainnet-"+address.substring(address.length()-8)+".txt");
        try{startActivityForResult(intent,EXPORT);}catch(android.content.ActivityNotFoundException ex){exportData=null;exportAction=null;showDocumentError(true);}
    }
    private void openBackup(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);
        try{startActivityForResult(intent,IMPORT);}catch(android.content.ActivityNotFoundException ex){error("此设备没有文件选择工具，请改用 NFC 卡片读取。");}
    }
    @Override protected void onActivityResult(int request,int result,Intent intent){
        super.onActivityResult(request,result,intent);
        if(request==EXPORT_QR){finishQrImageExport(result,intent);return;}
        if(request!=EXPORT&&request!=IMPORT)return;
        if(result!=RESULT_OK||intent==null||intent.getData()==null){exportData=null;exportAction=null;return;}
        android.net.Uri uri=intent.getData();
        if(request==EXPORT){
            String data=exportData,then=exportAction;CreationSession owner=session;exportData=null;exportAction=null;
            if(data!=null&&owner!=null)startDocumentOperation(uri,DocumentIo.Kind.BACKUP,data,null,owner,then,false);
        }else startDocumentOperation(uri,DocumentIo.Kind.IMPORT,null,null,null,null,false);
    }
    private void startDocumentOperation(android.net.Uri uri,DocumentIo.Kind kind,String encrypted,byte[] png,CreationSession owner,String then,boolean privateImage){
        startDocumentOperation(uri,kind,encrypted,png,owner,then,privateImage,Long.MAX_VALUE);
    }
    private void startDocumentOperation(android.net.Uri uri,DocumentIo.Kind kind,String encrypted,byte[] png,CreationSession owner,String then,boolean privateImage,long maximumDeadline){
        cancelDocumentOperation(true);dismissFileError();fileBusy=true;fileOwner=owner;fileThen=then;filePayload=encrypted;filePrivate=privateImage;
        android.content.Context appContext=getApplicationContext();
        fileJob=DocumentIo.start(this,uri,kind,encrypted,png,this,maximumDeadline,()->!privateImage||!AccessibilityGate.blocked(appContext));DocumentIo.Job active=fileJob;
        handler.postDelayed(expireFileTask,Math.max(1,active.deadline-android.os.SystemClock.elapsedRealtime()));
        whenResumed(()->{
            if(fileJob!=active||isFinishing()||isDestroyed())return;
            fileProgress=new AlertDialog.Builder(this).setTitle(kind==DocumentIo.Kind.IMPORT?R.string.file_io_importing:R.string.file_io_saving)
                .setMessage(R.string.file_io_wait).setNegativeButton(Texts.tr(this,"取消"),(d,w)->{if(fileJob==active)cancelDocumentOperation(false);}).create();
            fileProgress.setOnCancelListener(d->{if(fileJob==active)cancelDocumentOperation(false);});fileProgress.setCanceledOnTouchOutside(false);fileProgress.show();secure(fileProgress);
        });
    }
    private void dismissFileProgress(){AlertDialog owner=fileProgress;fileProgress=null;if(owner!=null)owner.dismiss();}
    private void cancelDocumentOperation(boolean quiet){
        DocumentIo.Job job=fileJob;if(job==null)return;
        boolean saving=job.kind!=DocumentIo.Kind.IMPORT;fileJob=null;fileBusy=false;fileOwner=null;fileThen=null;filePayload=null;filePrivate=false;
        handler.removeCallbacks(expireFileTask);dismissFileProgress();job.cancel();
        if(!quiet)whenResumed(()->showDocumentError(saving));
    }
    @Override public void documentFinished(DocumentIo.Job job,boolean success,String imported){
        if(fileJob!=job||isDestroyed()||isFinishing())return;
        if(filePrivate&&!allowSecretOperation())return;
        if(job.expired()){cancelDocumentOperation(false);return;}
        CreationSession owner=fileOwner;String then=fileThen,data=filePayload;boolean privateImage=filePrivate;
        fileJob=null;fileBusy=false;fileOwner=null;fileThen=null;filePayload=null;filePrivate=false;handler.removeCallbacks(expireFileTask);dismissFileProgress();
        if(!success){whenResumed(()->showDocumentError(job.kind!=DocumentIo.Kind.IMPORT));return;}
        if(job.kind==DocumentIo.Kind.IMPORT){whenResumed(()->acceptIncoming(imported));return;}
        if(job.kind==DocumentIo.Kind.BACKUP){
            if(session!=owner||owner==null||!owner.encrypted.equals(data))return;
            owner.markBackupSaved(data);
            rememberCreatedWallet(owner);
            whenResumed(()->{if("copy".equals(then))anotherCard();else if("home".equals(then))leaveCreation();else render();toast("加密备份已保存并核对");});
        }else whenResumed(()->toast(privateImage?"私钥二维码图片已保存，请妥善保管":"收款地址二维码图片已保存"));
    }
    private void showDocumentError(boolean saving){
        if(!resumed||isDestroyed()||isFinishing())return;dismissFileError();
        AlertDialog owner=new AlertDialog.Builder(this).setTitle(saving?R.string.file_io_save_failed:R.string.file_io_import_failed)
            .setMessage(saving?R.string.file_io_save_error:R.string.file_io_import_error).setPositiveButton(R.string.auto_lock_ok,(d,w)->{}).create();
        fileErrorDialog=owner;owner.setCancelable(false);owner.setOnDismissListener(d->{if(fileErrorDialog==owner)fileErrorDialog=null;});owner.show();secure(owner);
    }
    private void dismissFileError(){AlertDialog owner=fileErrorDialog;fileErrorDialog=null;if(owner!=null)owner.dismiss();}
    private void arm(Mode desired){showNfcPrompt(desired);}
    @Override public void readerReady(long token,NfcReaderControl.State state){
        if(token!=epoch||!resumed||mode==Mode.NONE||!readerPending)return;
        readerPending=false;handler.removeCallbacks(expireReaderStart);
        if(state==NfcReaderControl.State.READY)return;
        cancelNfc();
        if(state==NfcReaderControl.State.DISABLED){
            closeSheet();sheet=ui.sheet("请开启 NFC",p->{ui.note(p,"开启 NFC 后返回，再点击操作按钮。");ui.action(p,"打开 NFC 设置",true,0,()->{closeSheet();startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));});});
        }else showNfcError(state==NfcReaderControl.State.UNSUPPORTED?tr("此设备不支持 NFC，读取时可改用加密备份文件。"):getString(R.string.nfc_operation_busy));
    }
    private void showNfcPrompt(Mode desired){
        dismissNfc();mode=desired;updateReader();
        String title=desired==Mode.LOCK?"贴回同一张卡片，永久锁定":desired==Mode.VERIFY_LOCK?"请先移开，再贴卡核验锁定":desired==Mode.WRITE?copy.ui("writeArmed"):desired==Mode.CHECK?copy.ui("scanWrite"):copy.ui("scanRead");
        nfcDialog=new NfcScanDialog(ui,title,desired==Mode.WRITE?"贴回刚才检查的同一张卡片，保持贴合直到读回核对完成。":"将卡片贴近手机背面的 NFC 感应区。",
            desired==Mode.WRITE?"等待贴回同一张卡片…":"等待识别卡片…",this::cancelNfc);
    }
    private void cancelNfc(){
        boolean uncertainWrite=mode==Mode.WRITE&&nfcJob!=null;
        boolean uncertainLock=mode==Mode.LOCK||mode==Mode.VERIFY_LOCK;
        String lockDetail=uncertainLock?lockDiagnostic(mode,nfcJob,new java.util.concurrent.CancellationException()):null;
        if(uncertainLock){lockStatus="unknown";screen="done";}
        if(mode==Mode.WRITE||mode==Mode.CHECK)clearCandidate();
        epoch++;stopNfcJob();mode=Mode.NONE;dismissNfc();updateReader();if(screen.equals("done"))render();
        if(uncertainWrite)showNfcError(getString(R.string.nfc_write_unconfirmed));
        else if(uncertainLock)lockProblem(false,lockDetail);
    }
    private void expireReader(){
        if(readerPending&&resumed&&mode!=Mode.NONE){String stage=nfcReader==null?"NFC-Q":nfcReader.diagnostic();readerPending=false;cancelNfc();showNfcError(getString(R.string.nfc_startup_timeout,stage));}
    }
    private void expireNfc(){
        NfcIo.Job job=nfcJob;if(job==null)return;
        if(job.expired())nfcFinished(job,null,new NfcIo.Expired());else handler.postDelayed(expireNfcTask,job.remainingMillis());
    }
    private void stopNfcJob(){
        NfcIo.Job job=nfcJob;nfcJob=null;nfcBusy=false;nfcOperation=Mode.NONE;nfcOwner=null;
        handler.removeCallbacks(expireNfcTask);if(job!=null)job.cancel();
    }
    private void dismissNfc(){if(nfcDialog!=null){Dialog d=nfcDialog;nfcDialog=null;d.dismiss();}}
    private void updateReader(){
        if(nfcReader==null)return;
        boolean enable=resumed&&mode!=Mode.NONE;
        readerPending=enable;handler.removeCallbacks(expireReaderStart);
        if(enable)handler.postDelayed(expireReaderStart,5000);
        nfcReader.request(enable,epoch);
    }
    @Override public void onTagDiscovered(Tag tag){int token=epoch;Mode detected=mode;handler.post(()->{if(epoch==token&&mode==detected)startNfcOperation(tag);});}
    private void startNfcOperation(Tag tag){
        if(!resumed||nfcBusy||mode==Mode.NONE)return;
        readerPending=false;handler.removeCallbacks(expireReaderStart);
        nfcOperation=mode;nfcToken=epoch;nfcOwner=session;nfcProtect=lockAfterWrite;nfcBusy=true;
        Mode operation=nfcOperation;CreationSession owner=nfcOwner;
        if(nfcDialog!=null)nfcDialog.processing((operation==Mode.LOCK||operation==Mode.VERIFY_LOCK)?copy.section("lockCopy").optString("scan"):operation==Mode.WRITE?"正在写入并核对":operation==Mode.CHECK?"正在检查卡片":"正在读取冷钱包",
            operation==Mode.WRITE?"写入 → 读回 → 核对内容，请保持贴合…":"正在读取，请保持卡片贴合…");
        boolean duplicate=copyWriting&&owner!=null&&owner.containsCard(tag.getId());
        NfcIo.Work task=NfcCards.task(tag,operation.name(),owner==null?null:owner.encrypted,candidate,nfcProtect,duplicate);
        nfcJob=NfcIo.start(task,this,handler::post);
        handler.postDelayed(expireNfcTask,nfcJob.remainingMillis());
    }
    @Override public void nfcFinished(NfcIo.Job job,Object value,Exception failure){
        NfcCards.Result result=value instanceof NfcCards.Result?(NfcCards.Result)value:null;
        if(nfcJob!=job){if(result!=null)result.clear();return;}
        Mode operation=nfcOperation;int token=nfcToken;CreationSession owner=nfcOwner;boolean protect=nfcProtect;
        boolean stale=epoch!=token||!resumed||(operation!=Mode.READ&&session!=owner);
        if(job.expired())failure=new NfcIo.Expired();
        nfcJob=null;nfcOwner=null;nfcOperation=Mode.NONE;nfcBusy=false;handler.removeCallbacks(expireNfcTask);
        if(stale||failure!=null)job.cancel();
        try{
            if(stale)return;
            epoch++;mode=Mode.NONE;dismissNfc();updateReader();
            if(failure!=null){
                if(operation==Mode.LOCK||operation==Mode.VERIFY_LOCK){lockStatus="unknown";screen="done";render();lockProblem(false,lockDiagnostic(operation,job,failure));}
                else{
                    if(operation==Mode.WRITE){clearCandidate();render();}else if(operation==Mode.CHECK)clearCandidate();
                    if(operation==Mode.CHECK&&protect&&failure.getMessage()!=null&&(failure.getMessage().contains("不支持")||failure.getMessage().contains("镜像或访问")))lockProblem(true);
                    else showNfcError(failure instanceof NfcCards.ReadOnlyCard?getString(R.string.nfc_card_read_only):failure instanceof NfcIo.Expired?getString(operation==Mode.WRITE?R.string.nfc_write_unconfirmed:R.string.nfc_operation_timeout):failure instanceof NfcIo.Unavailable?getString(R.string.nfc_operation_busy):nfcError(failure));
                }
                return;
            }
            if(result==null){showNfcError(getString(R.string.nfc_operation_timeout));return;}
            if(operation==Mode.READ)acceptIncoming(result.text,result.cardId);
            else if(operation==Mode.CHECK){clearCandidate();candidate=result.takeCandidate();confirmCard(candidate);}
            else if(operation==Mode.WRITE){owner.markCardWritten(result.uid);copyWriting=false;screen="done";lockStatus=protect?"unprotected":"";render();haptic(true);if(protect)confirmReadOnly();}
            else if(operation==Mode.LOCK){lockStatus="unknown";screen="done";render();arm(Mode.VERIFY_LOCK);}
            else if(operation==Mode.VERIFY_LOCK){lockStatus="locked";screen="done";render();haptic(true);}
        }finally{if(result!=null)result.clear();}
    }
    private void showNfcError(String message){
        closeSheet();sheet=ui.sheet(getString(R.string.nfc_operation_problem),p->{
            ui.note(p,message);ui.action(p,getString(R.string.auto_lock_dismiss),true,0,this::closeSheet);
        });
    }
    private String lockDiagnostic(Mode operation,NfcIo.Job job,Exception failure){
        String kind;int reason;
        if(failure instanceof NfcIo.Expired){kind="TIME";reason=R.string.nfc_lock_reason_timeout;}
        else if(failure instanceof NfcIo.Unavailable){kind="BUSY";reason=R.string.nfc_lock_reason_busy;}
        else if(failure instanceof android.nfc.TagLostException){kind="LOST";reason=R.string.nfc_lock_reason_lost;}
        else if(failure instanceof java.util.concurrent.CancellationException){kind="CANCEL";reason=R.string.nfc_lock_reason_cancel;}
        else if(failure instanceof java.io.IOException){kind="IO";reason=R.string.nfc_lock_reason_io;}
        else{kind=failure instanceof IllegalArgumentException?"CARD":failure instanceof IllegalStateException?"STATE":"OTHER";reason=R.string.nfc_lock_reason_check;}
        String code="NFC-"+(operation==Mode.VERIFY_LOCK?"VERIFY":"LOCK")+"-"+(job==null?NfcIo.Phase.START:job.phase()).code+"-"+kind;
        return getString(R.string.nfc_lock_diagnostic,code,getString(reason));
    }
    private void lockProblem(boolean unsupported){lockProblem(unsupported,null);}
    private void lockProblem(boolean unsupported,String diagnostic){
        closeSheet();org.json.JSONObject c=copy.section("lockCopy");sheet=ui.sheet(c.optString(unsupported?"unsupported":"issue"),p->{
            ui.note(p,c.optString(unsupported?"unsupportedBody":"issueBody"));
            if(!unsupported&&diagnostic!=null)ui.callout(p,R.drawable.ic_warning,diagnostic);
            ui.action(p,c.optString(unsupported?"back":"backup"),true,0,()->{closeSheet();if(unsupported){screen="write";render();}else backupSheet(null);});
        });
    }
    private void confirmCard(NfcCards.Candidate result){
        closeSheet();sheet=ui.sheet(result.hasData?"卡片已有数据，是否覆盖？":copy.ui("cardReady"),p->{
            ui.note(p,result.hasData?"写入会覆盖这张卡片原有的全部 NDEF 内容。请确认没有需要保留的数据。":copy.ui("cardReadyBody"));
            if(lockAfterWrite)ui.callout(p,R.drawable.ic_lock,copy.section("lockCopy").optString("notice"));
            ui.helper(p,"所需容量 "+NfcCards.message(session.encrypted).toByteArray().length+" 字节 / 可用容量 "+result.capacity+" 字节");
            ui.action(p,result.hasData?"确认覆盖并写入":"确认写入",true,R.drawable.ic_nfc,()->{closeSheet();arm(Mode.WRITE);});ui.link(p,"取消",0,this::closeSheet);
        });
    }
    private String nfcError(Exception ex){
        if(ex instanceof android.nfc.TagLostException)return "卡片离开感应区，操作未确认完成。请重新贴卡；若曾开始写入，请先重新读卡检查。";
        if(ex instanceof IllegalArgumentException||ex instanceof IllegalStateException)return ex.getMessage();
        return "NFC 操作失败或中断，请重新贴卡并检查数据。";
    }
    private void bindPasswordDismiss(Dialog owner){
        bindPasswordDismiss(owner,owner.getWindow().getDecorView());
        owner.setOnCancelListener(d->{if(busy&&workDialog==owner)cancelWork();});
    }
    private void bindPasswordDismiss(Dialog owner,View view){
        if("sheet-close".equals(view.getTag())||"password-cancel".equals(view.getTag())){
            view.setOnClickListener(v->{
                if(!resumed||!owner.isShowing())return;
                if(busy){if(workDialog==owner)cancelWork();return;}
                if(owner==creationDialog)closeCreationDialog();else if(owner==unlockDialog)closeUnlockDialog();
            });
        }
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)bindPasswordDismiss(owner,group.getChildAt(i));}
    }
    private int begin(Dialog owner,Button submit,String label,int icon){
        busy=true;workDialog=owner;workSubmit=submit;workLabel=label;workIcon=icon;
        for(View button:ui.buttons)button.setEnabled(false);
        for(EditText input:ui.passwords){
            workHints.put(input,input.getHint());input.setEnabled(false);input.setHint("••••••••••••");
            input.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
            if(input.getParent() instanceof LinearLayout){LinearLayout row=(LinearLayout)input.getParent();for(int i=0;i<row.getChildCount();i++)if(row.getChildAt(i) instanceof ImageButton){ImageButton eye=(ImageButton)row.getChildAt(i);eye.setImageDrawable(ui.drawable(R.drawable.ic_eye_closed,AppUi.SUB,20));eye.setContentDescription(tr("显示")+input.getContentDescription());}}
        }
        enablePasswordDismiss(owner.getWindow().getDecorView());
        ui.loadingAction(submit,getString(R.string.crypto_processing));owner.setCanceledOnTouchOutside(false);return epoch;
    }
    private void enablePasswordDismiss(View view){
        if("sheet-close".equals(view.getTag())||"password-cancel".equals(view.getTag()))view.setEnabled(true);
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)enablePasswordDismiss(group.getChildAt(i));}
    }
    private void cancelWork(){
        Dialog owner=workDialog;epoch++;wipePendingPasswords();endWork();if(owner!=null)owner.dismiss();
        if(resumed&&screen.equals("setup"))render();toast("已取消，可以重新操作");
    }
    private void endWork(){
        busy=false;Dialog owner=workDialog;workDialog=null;
        for(View button:ui.buttons)button.setEnabled(true);
        if(workSubmit!=null)ui.primaryActionEnabled(workSubmit,workLabel,workIcon,true);
        workSubmit=null;workLabel=null;workIcon=0;
        java.util.Map<EditText,CharSequence> hints=new java.util.IdentityHashMap<>(workHints);workHints.clear();
        for(java.util.Map.Entry<EditText,CharSequence> entry:hints.entrySet()){
            EditText input=entry.getKey();input.setEnabled(true);input.setHint(entry.getValue());input.setText("");
        }
        if(owner!=null&&owner.isShowing())owner.setCanceledOnTouchOutside(true);
    }
    private void dismissWork(){Dialog owner=workDialog;if(owner!=null){endWork();owner.dismiss();}}
    private void failure(int token,Exception ex,String fallback){runOnUiThread(()->{
        if(epoch!=token||!resumed)return;endWork();
        if(screen.equals("setup")&&enhanced&&cameraEntropy==null){closeCreationDialog();render();}
        error(ex instanceof IllegalArgumentException?ex.getMessage():fallback);
    });}
    private void closeSheet(){if(pendingVerification!=null)closeUnlocked();closeUnlockDialog();closeCreationDialog();if(sheet!=null){Dialog d=sheet;sheet=null;d.dismiss();}}
    private void secure(Dialog dialog){WindowProtection.secure(dialog.getWindow());}
    private void error(String message){if(creationFeedback!=null){creationFeedback.setText(tr(message));creationFeedback.setVisibility(View.VISIBLE);}if(feedback!=null){feedback.setText(tr(message));feedback.setVisibility(View.VISIBLE);}toast(message);haptic(false);}
    private void toast(String message){Toast.makeText(this,tr(message),Toast.LENGTH_LONG).show();}
    private void copy(String label,String value){
        ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(clipboard==null){error("无法访问剪贴板，请重试");return;}
        publicClipboardMarker=label+" · BitTap-"+java.util.UUID.randomUUID();
        ClipData clip=ClipData.newPlainText(publicClipboardMarker,value);
        ownedClipboardMarkers.add(publicClipboardMarker);
        if(!VerifiedClipboard.writeAndVerify(clip,clipboard::setPrimaryClip,clipboard::getPrimaryClip)){copyUnconfirmed();return;}
        handler.removeCallbacks(clearPrivateClipboard);clipboardMarker=null;clipboardExpiry=0;if(feedback!=null)feedback.setVisibility(View.GONE);if(qrFeedback!=null)qrFeedback.setVisibility(View.GONE);toast(label+"已复制");
    }
    private void requestPrivateCopy(){
        if(!allowSecretOperation())return;
        if(expireUnlockedIfNeeded())return;
        if(unlocked==null||!resumed||copyConfirm!=null)return;
        closeQrSaveConfirm();Dialog owner=new Dialog(this);copyConfirm=owner;
        owner.setOnDismissListener(d->{if(copyConfirm==owner){copyConfirm=null;copyConfirmFeedback=null;}});
        ui.showSheet(owner,getString(R.string.copy_confirm_title),p->{
            TextView description=ui.text(p,getString(R.string.copy_confirm_description),14,AppUi.SUB,false);ui.line(description,1.8f);
            LinearLayout warning=ui.callout(p,R.drawable.ic_warning,null),box=(LinearLayout)warning.getParent();
            android.graphics.drawable.GradientDrawable background=ui.shape(0xfffff0e9,13,false);background.setStroke(ui.borderWidth(),0xfff2d2c2);box.setBackground(background);
            ((ImageView)box.getChildAt(0)).setImageDrawable(ui.drawable(R.drawable.ic_warning,0xff99412c,17));
            TextView caution=ui.text(warning,getString(R.string.copy_confirm_caution),14,0xff99412c,false);ui.line(caution,1.75f);
            ui.space(p,19);TextView transfer=ui.text(p,getString(R.string.copy_confirm_transfer),13,AppUi.SUB,false);ui.line(transfer,1.8f);ui.space(p,13);
            copyConfirmFeedback=ui.text(p,"",13,AppUi.ERROR,false);copyConfirmFeedback.setVisibility(View.GONE);copyConfirmFeedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            ui.action(p,getString(R.string.copy_confirm_action),true,R.drawable.ic_copy,()->{
                // A dismissed/backgrounded dialog can never authorize a later wallet or a stale click.
                if(copyConfirm!=owner||!owner.isShowing()||!resumed||unlocked==null)return;
                if(copyPrivateKey())closeCopyConfirm();
                else if(copyConfirmFeedback!=null){copyConfirmFeedback.setText(tr("未能确认复制成功，请检查系统剪贴板权限后重试。"));copyConfirmFeedback.setVisibility(View.VISIBLE);}
            });
            ui.link(p,"取消",0,this::closeCopyConfirm);
        },this::closeCopyConfirm,R.drawable.ic_copy);secure(owner);
    }
    private void closeCopyConfirm(){
        Dialog owner=copyConfirm;copyConfirm=null;copyConfirmFeedback=null;if(owner!=null)owner.dismiss();
    }
    private boolean copyPrivateKey(){
        if(!allowSecretOperation())return false;
        if(expireUnlockedIfNeeded())return false;
        if(unlocked==null||!resumed||secretView==null||secretView.isCleared()||qrDialog==null||!qrDialog.isShowing())return false;ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(clipboard==null){error("无法访问剪贴板，请重试");return false;}
        char[] text=secretView.copyCharacters();ClipData clip;
        try{publicClipboardMarker=null;clipboardMarker="BitTap-private-"+java.util.UUID.randomUUID();clip=ClipData.newPlainText(clipboardMarker,new String(text));}
        finally{WalletCrypto.wipe(text);}
        PersistableBundle extras=new PersistableBundle();extras.putBoolean("android.content.extra.IS_SENSITIVE",true);clip.getDescription().setExtras(extras);
        ownedClipboardMarkers.add(clipboardMarker);clipboardExpiry=android.os.SystemClock.elapsedRealtime()+120000;
        handler.removeCallbacks(clearPrivateClipboard);handler.postDelayed(clearPrivateClipboard,120000);
        if(!VerifiedClipboard.writeAndVerify(clip,clipboard::setPrimaryClip,clipboard::getPrimaryClip)){copyUnconfirmed();return false;}
        if(feedback!=null)feedback.setVisibility(View.GONE);if(qrFeedback!=null)qrFeedback.setVisibility(View.GONE);toast("WIF 私钥已复制，请及时导入自己的钱包，切勿分享");return true;
    }
    private void copyUnconfirmed(){
        String message="未能确认复制成功，请检查系统剪贴板权限后重试。";
        if(qrFeedback!=null){qrFeedback.setText(tr(message));qrFeedback.setVisibility(View.VISIBLE);}error(message);
    }
    private void expirePrivateClipboard(){
        if(clipboardMarker==null||android.os.SystemClock.elapsedRealtime()<clipboardExpiry)return;
        try{
            ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);ClipData clip=clipboard==null?null:clipboard.getPrimaryClip();
            CharSequence label=clip==null?null:clip.getDescription().getLabel();
            if(label!=null&&label.toString().startsWith("BitTap-private-")&&ownedClipboardMarkers.contains(label.toString()))clipboard.clearPrimaryClip();
        }catch(RuntimeException ignored){/* Retry on resume; do not erase unrelated clipboard data. */}
    }
    private void clearOwnedClipboard(){
        try{
            ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);ClipData clip=clipboard==null?null:clipboard.getPrimaryClip();
            if(clip!=null){CharSequence label=clip.getDescription().getLabel();
                if(label!=null&&ownedClipboardMarkers.contains(label.toString()))clipboard.clearPrimaryClip();
            }
        }catch(RuntimeException ignored){/* The OS may deny background access; no unrelated clipboard is erased. */}
    }
    private synchronized void trackPassword(char[] password){pendingPasswords.add(password);}
    private synchronized void forgetPassword(char[] password){WalletCrypto.wipe(password);pendingPasswords.remove(password);}
    private synchronized void wipePendingPasswords(){for(char[] value:pendingPasswords)WalletCrypto.wipe(value);pendingPasswords.clear();}
    private synchronized void trackPrivateView(char[] value){pendingPrivateViews.add(value);}
    private synchronized void forgetPrivateView(char[] value){WalletCrypto.wipe(value);pendingPrivateViews.remove(value);}
    private synchronized void wipePendingPrivateViews(){for(char[] value:pendingPrivateViews)WalletCrypto.wipe(value);pendingPrivateViews.clear();}
    private void clearCandidate(){if(candidate!=null){WalletCrypto.wipe(candidate.uid);WalletCrypto.wipe(candidate.previous);candidate=null;}}
    private void clearWalletSession(boolean clipboard){
        closeAccessNotice();
        dismissAutoLockDialog();dismissFileError();cancelDocumentOperation(true);
        epoch++;stopNfcJob();mode=Mode.NONE;handler.removeCallbacksAndMessages(null);afterResume=null;clearCamera();setupNote="";enhanced=false;lockAfterWrite=false;lockStatus="";protectionReturn="home";
        closeSheet();dismissNfc();dismissWork();dismissQr();hideSecret.run();closeUnlocked();wipePendingPasswords();
        if(ui!=null){ui.clearInputs();ui.buttons.clear();}
        clearCandidate();clearPendingQrImage();clearIncomingVerification();recordedCreation=null;recordedCreationCards=0;creationRecordSaving=false;recordRows=java.util.Collections.emptyList();session=null;incoming=null;incomingCardId=null;exportData=null;exportAction=null;copyWriting=false;
        secretView=null;secretToggle=null;
        if(clipboard)clearOwnedClipboard();clipboardMarker=null;publicClipboardMarker=null;clipboardExpiry=0;ownedClipboardMarkers.clear();
    }
    private void showAddressQr(String type,String address,boolean verified){
        if(!receivingAllowed(address,verified)){
            if(session!=null&&(screen.equals("write")||screen.equals("done"))){if(session.cardCount()>0)backupSheet(null);else toast(getString(R.string.verification_finish_first));}
            else if(incoming!=null&&screen.equals("unlock")){showUnlockDialog();pendingAddressType=type;}
            return;
        }
        try{
            dismissQr();int size=qrSize();
            showQrSheet(type,address,verified,false,QrCodeEncoder.address(address,size),size);
        }catch(Exception ex){dismissQr();error("地址二维码生成失败，请重试");}
    }
    private int qrSize(){return Math.max(160,Math.min(ui.dp(240),getResources().getDisplayMetrics().widthPixels-ui.dp(104)));}
    private void showPrivateQr(){
        if(!allowSecretOperation())return;
        if(expireUnlockedIfNeeded()||unlocked==null||pendingVerification!=null)return;
        if(!resumed||privateOpening)return;dismissQr();PrivateSession owner=unlocked;int token=epoch;privateOpening=true;
        worker.execute(()->{
            char[] value=null;
            try{
                value=owner.wif().toCharArray();if(epoch!=token||!resumed)return;char[] text=value;trackPrivateView(text);
                runOnUiThread(()->{
                    try{
                        if(epoch!=token||!resumed||unlocked!=owner||expireUnlockedIfNeeded()||!allowSecretOperation())return;
                        privateOpening=false;int size=Math.min(qrSize(),ui.dp(180));
                        showQrSheet("WIF 私钥",null,false,true,QrCodeEncoder.wif(new String(text),size),size,text);
                    }catch(Exception ex){dismissQr();error(getString(R.string.private_qr_retry));}
                    finally{forgetPrivateView(text);if(epoch==token)privateOpening=false;}
                });value=null;
            }catch(Exception ex){runOnUiThread(()->{if(epoch==token){privateOpening=false;closeUnlocked();screen="unlock";render();error(getString(R.string.private_qr_retry));}});}
            finally{WalletCrypto.wipe(value);}
        });
    }
    private void showQrSheet(String type,String address,boolean verified,boolean privateKey,BitMatrix matrix,int size){
        showQrSheet(type,address,verified,privateKey,matrix,size,null);
    }
    private void showQrSheet(String type,String address,boolean verified,boolean privateKey,BitMatrix matrix,int size,char[] privateText){
        Dialog owner=new Dialog(this);qrDialog=owner;
        owner.setOnDismissListener(d->{if(qrDialog==owner)clearQrBitmap();});
        ui.showSheet(owner,privateKey?getString(R.string.wallet_view_private):"收款地址",p->{
            if(privateKey)ui.note(p,getString(R.string.wallet_private_description));
            LinearLayout card=ui.card(p);card.setBackground(ui.shape(AppUi.BG,18,true));
            LinearLayout top=ui.horizontal();card.addView(top);
            TextView name=ui.text(top,type.replace(" · 推荐","").replace(" · 需确认恢复兼容",""),11,AppUi.SUB,false);
            name.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            TextView network=new TextView(this);network.setText(R.string.btc_mainnet);network.setTextSize(11);network.setTextColor(AppUi.ACCENT);
            network.setPadding(0,0,0,0);top.addView(network);
            ui.space(card,8);card.addView(qrPanel(matrix,size,privateKey?"WIF 私钥二维码，仅供自己的钱包扫入":"BTC 收款地址二维码"));
            if(privateKey){
                if(privateText==null)throw new IllegalStateException("Private display requires a live action buffer");
                secretView=new PrivateKeyView(this,privateText);
                secretView.setBackground(ui.shape(0xfff5efe6,11,false));
                card.addView(secretView,new LinearLayout.LayoutParams(-1,-2));
                if(Build.VERSION.SDK_INT>=34)qrImage.setAccessibilityDataSensitive(View.ACCESSIBILITY_DATA_SENSITIVE_YES);
                qrImage.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
                qrImage.setImportantForContentCapture(View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS);
            }else{
                TextView value=ui.text(card,"",12,AppUi.FG,false);value.setText(address);value.setTypeface(Typeface.MONOSPACE);value.setGravity(Gravity.CENTER);
                value.setSingleLine(false);value.setHorizontallyScrolling(false);value.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
                value.setSaveEnabled(false);value.setTextIsSelectable(false);value.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
                value.setPadding(0,ui.dp(12),0,ui.dp(12));
            }
            if(privateKey){
                ui.callout(p,R.drawable.ic_key,getString(R.string.wallet_private_warning));
            }
            else if(!verified){ui.callout(p,R.drawable.ic_warning,copy.ui("qrUnverified"));}
            else{
                LinearLayout status=ui.horizontal();status.setGravity(Gravity.CENTER);status.setPadding(0,ui.dp(12),0,ui.dp(12));p.addView(status);
                ImageView check=new ImageView(this);check.setImageDrawable(ui.drawable(verified?R.drawable.ic_check:R.drawable.ic_lock,AppUi.ACCENT,17));
                status.addView(check,new LinearLayout.LayoutParams(ui.dp(18),ui.dp(18)));
                TextView text=new TextView(this);text.setIncludeFontPadding(false);text.setText(verificationSource.equals("local")?getString(R.string.verification_local):tr("已与私钥核对，地址正确"));text.setTextColor(AppUi.ACCENT);text.setTextSize(12);text.setPadding(ui.dp(8),0,0,0);status.addView(text);
            }
            qrFeedback=ui.text(p,"",12,AppUi.ERROR,false);qrFeedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);qrFeedback.setVisibility(View.GONE);
            ui.space(p,8);LinearLayout actions=ui.horizontal();actions.setBaselineAligned(false);p.addView(actions);
            Button copy=ui.action(actions,privateKey?MainActivity.this.copy.ui("copyWif"):MainActivity.this.copy.ui("copyAddress"),!privateKey,R.drawable.ic_copy,privateKey?this::requestPrivateCopy:()->{if(currentQr(owner)&&receivingAllowed(address,true))copy("收款地址",address);});
            Button save=ui.action(actions,"保存图片",privateKey,R.drawable.ic_download,()->requestQrImageSave(owner,privateKey,address,verified));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.rightMargin=ui.dp(8);copy.setLayoutParams(cp);
            LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-2,1);save.setLayoutParams(sp);
            copy.setTextSize(12);save.setTextSize(12);copy.setPadding(ui.dp(8),ui.dp(12),ui.dp(8),ui.dp(12));save.setPadding(ui.dp(8),ui.dp(12),ui.dp(8),ui.dp(12));
            if(privateKey){
                ui.space(p,17);ui.divider(p);LinearLayout help=ui.horizontal();help.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);p.addView(help);
                help.addView(ui.inline(getString(R.string.wallet_transfer_help),R.drawable.ic_help,this::transferHelp));
            }else{
                ui.link(p,MainActivity.this.copy.ui("viewBalance"),R.drawable.ic_external,()->explorers(type,address,verified));
                TextView footer=ui.text(p,MainActivity.this.copy.ui("qrPublic"),11,AppUi.SUB,false);footer.setGravity(Gravity.CENTER);footer.setPadding(0,ui.dp(14),0,0);
            }
        },this::dismissQr,0,privateKey);
        secure(owner);
    }
    private LinearLayout qrPanel(BitMatrix matrix,int size,String description){
        int[] pixels=new int[size*size];
        try{
            for(int y=0;y<size;y++)for(int x=0;x<size;x++)pixels[y*size+x]=matrix.get(x,y)?Color.BLACK:Color.WHITE;
            qrBitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);qrBitmap.setPixels(pixels,0,size,0,0,size,size);
        }finally{Arrays.fill(pixels,0);matrix.clear();}
        LinearLayout p=ui.vertical();p.setGravity(Gravity.CENTER);
        qrImage=new ImageView(this);qrImage.setImageBitmap(qrBitmap);qrImage.setContentDescription(description);qrImage.setBackgroundColor(Color.WHITE);qrImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        p.addView(qrImage,new LinearLayout.LayoutParams(size,size));return p;
    }
    private boolean currentQr(Dialog owner){return qrDialog==owner&&owner.isShowing()&&qrBitmap!=null&&resumed&&!busy&&!fileBusy;}
    private void requestQrImageSave(Dialog qrOwner,boolean privateKey,String address,boolean verified){
        if(!privateKey&&!receivingAllowed(address,verified))return;
        if(privateKey&&!allowSecretOperation())return;
        if(privateKey&&expireUnlockedIfNeeded())return;
        if(!currentQr(qrOwner)||pendingQrImage!=null||qrSaveConfirm!=null)return;
        closeCopyConfirm();
        if(!privateKey&&verified){beginQrImageExport(false,address,true);return;}
        if(privateKey&&unlocked==null)return;
        Dialog owner=new Dialog(this);qrSaveConfirm=owner;int token=epoch;Bitmap original=qrBitmap;
        owner.setOnDismissListener(d->{if(qrSaveConfirm==owner)qrSaveConfirm=null;});
        Runnable cancel=()->{if(qrSaveConfirm==owner)closeQrSaveConfirm();};
        ui.showSheet(owner,privateKey?"保存私钥二维码？":getString(R.string.address_save_unverified_title),p->{
            ui.note(p,privateKey?"图片包含明文私钥。任何获得图片的人，都可能转走钱包中的 BTC。请保存到可信的位置，避免上传或分享。":getString(R.string.address_save_unverified_body));
            if(!privateKey)ui.callout(p,R.drawable.ic_password_warning,getString(R.string.address_save_unverified_note));
            ui.action(p,privateKey?"确认保存":getString(R.string.address_save_unverified_continue),true,R.drawable.ic_download,()->{
                if(qrSaveConfirm!=owner||!owner.isShowing()||!currentQr(qrOwner)||epoch!=token||qrBitmap!=original)return;
                closeQrSaveConfirm();beginQrImageExport(privateKey,address,verified);
            });
            ui.link(p,"取消",0,cancel);
        },cancel,privateKey?0:R.drawable.ic_password_warning);secure(owner);
    }
    private void beginQrImageExport(boolean privateKey,String address,boolean verified){
        if(!privateKey&&!receivingAllowed(address,verified))return;
        if(privateKey&&!allowSecretOperation())return;
        if(privateKey&&expireUnlockedIfNeeded())return;
        if(qrBitmap==null||!resumed||pendingQrImage!=null||(privateKey&&unlocked==null))return;
        try{
            char[] text=privateKey&&secretView!=null&&!secretView.isCleared()?secretView.copyCharacters():null;
            Bitmap artwork;
            try{if(privateKey&&text==null)throw new IllegalStateException("Private display is closed");artwork=privateKey?PrivateImageRenderer.render(Texts.wrap(this),new String(text)):AddressImageRenderer.render(Texts.wrap(this),address,verified);}
            finally{WalletCrypto.wipe(text);}
            try{pendingQrImage=QrImageFiles.encode(artwork);}
            finally{artwork.eraseColor(Color.WHITE);artwork.recycle();}
            pendingQrPrivate=privateKey;
            if(privateKey&&expireUnlockedIfNeeded())return;
            long now=android.os.SystemClock.elapsedRealtime();
            pendingQrDeadline=privateKey?now+unlockDeadline.remaining(now):now+180000;
            handler.postDelayed(expireQrExport,Math.max(1,pendingQrDeadline-now));
            Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.setType("image/png");intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_TITLE,privateKey?"bittap-private-key-qr.png":"bittap-address-qr.png");
            startActivityForResult(intent,EXPORT_QR);
        }catch(Exception ex){clearPendingQrImage();showDocumentError(true);}
    }
    private void finishQrImageExport(int result,Intent intent){
        if(pendingQrPrivate&&!allowSecretOperation()){clearPendingQrImage();return;}
        if(result!=RESULT_OK||intent==null||intent.getData()==null){clearPendingQrImage();whenResumed(()->toast("已取消保存图片"));return;}
        byte[] png=pendingQrImage;boolean privateKey=pendingQrPrivate;long deadline=pendingQrDeadline;
        pendingQrImage=null;pendingQrPrivate=false;pendingQrDeadline=0;handler.removeCallbacks(expireQrExport);
        if(png==null){whenResumed(()->showDocumentError(true));return;}
        if(privateKey&&deadline>0&&android.os.SystemClock.elapsedRealtime()>=deadline){Arrays.fill(png,(byte)0);whenResumed(()->showDocumentError(true));return;}
        startDocumentOperation(intent.getData(),DocumentIo.Kind.PNG,null,png,null,null,privateKey,privateKey&&deadline>0?deadline:Long.MAX_VALUE);
    }
    private void clearPendingQrImage(){
        handler.removeCallbacks(expireQrExport);if(pendingQrImage!=null){Arrays.fill(pendingQrImage,(byte)0);pendingQrImage=null;}pendingQrPrivate=false;pendingQrDeadline=0;
    }
    private void closeQrSaveConfirm(){if(qrSaveConfirm!=null){Dialog d=qrSaveConfirm;qrSaveConfirm=null;d.dismiss();}}
    private void clearQrBitmap(){
        closeCopyConfirm();closeQrSaveConfirm();qrFeedback=null;
        hideSecret.run();secretView=null;
        if(qrImage!=null){qrImage.setImageBitmap(null);qrImage=null;}
        if(qrBitmap!=null){qrBitmap.eraseColor(Color.WHITE);qrBitmap.recycle();qrBitmap=null;}qrDialog=null;
    }
    private void dismissQr(){if(qrDialog!=null)qrDialog.dismiss();clearQrBitmap();}
    private void closeUnlocked(){pendingVerification=null;privateOpening=false;wipePendingPrivateViews();unlockDeadline.clear();handler.removeCallbacks(checkUnlockDeadline);closeCopyConfirm();handler.removeCallbacks(hideSecret);hideSecret.run();if(unlocked!=null){unlocked.close();unlocked=null;}}
    /** Called only by accepted touch/key events or semantic button actions, never by redraws. */
    private boolean privateInteraction(){
        if(unlocked==null||!unlockDeadline.active())return false;
        if(!unlockDeadline.interact(android.os.SystemClock.elapsedRealtime())){handler.post(checkUnlockDeadline);return true;}
        return false;
    }
    private boolean expireUnlockedIfNeeded(){
        if(unlocked==null||!unlockDeadline.active())return false;
        UnlockDeadline.Cause cause=unlockDeadline.cause(android.os.SystemClock.elapsedRealtime());
        if(cause==UnlockDeadline.Cause.NONE)return false;
        epoch++;closeSheet();dismissQr();clearPendingQrImage();ui.clearPasswords();wipePendingPasswords();closeUnlocked();
        clipboardExpiry=0;expirePrivateClipboard();screen="unlock";render();
        if(resumed){
            dismissAutoLockDialog();Dialog owner=new Dialog(this);autoLockDialog=owner;
            ui.statusSheet(owner,getString(R.string.auto_lock_title),R.drawable.ic_lock,p->{
                TextView reason=ui.text(p,getString(cause==UnlockDeadline.Cause.IDLE?R.string.auto_lock_idle_body:R.string.auto_lock_max_body),15,AppUi.SUB,false);ui.line(reason,1.9f);
                ui.callout(p,R.drawable.ic_shield,getString(currentVerification()!=null?R.string.verification_locked_body:R.string.auto_lock_cleared));ui.space(p,17);
                Button dismiss=ui.action(p,getString(R.string.auto_lock_dismiss),true,0,()->{});
                dismiss.setOnClickListener(v->{if(autoLockDialog==owner&&owner.isShowing())owner.dismiss();});
            });
            owner.setCancelable(false);owner.setCanceledOnTouchOutside(false);owner.setOnDismissListener(d->{if(autoLockDialog==owner)autoLockDialog=null;});secure(owner);
        }
        return true;
    }
    private void tickUnlockedSession(){
        handler.removeCallbacks(checkUnlockDeadline);
        if(expireUnlockedIfNeeded()||unlocked==null||!unlockDeadline.active())return;
        handler.postDelayed(checkUnlockDeadline,Math.min(1000,Math.max(1,unlockDeadline.remaining(android.os.SystemClock.elapsedRealtime()))));
    }
    private void dismissAutoLockDialog(){Dialog owner=autoLockDialog;autoLockDialog=null;if(owner!=null)owner.dismiss();}
    private char[] chars(EditText input){char[] a=new char[input.length()];input.getText().getChars(0,input.length(),a,0);return a;}
    private void hideKeyboard(){InputMethodManager imm=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);Dialog owner=workDialog!=null?workDialog:unlockDialog!=null?unlockDialog:creationDialog;View v=owner!=null?owner.getCurrentFocus():getCurrentFocus();if(imm!=null&&v!=null)imm.hideSoftInputFromWindow(v.getWindowToken(),0);}
    private void tap(){privateInteraction();getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}
    private void haptic(boolean success){getWindow().getDecorView().performHapticFeedback(success?HapticFeedbackConstants.CONFIRM:HapticFeedbackConstants.REJECT);}
}
