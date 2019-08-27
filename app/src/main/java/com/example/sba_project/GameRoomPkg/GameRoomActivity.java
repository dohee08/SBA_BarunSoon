package com.example.sba_project.GameRoomPkg;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.sba_project.Adapter.PlayerItem;
import com.example.sba_project.Util.Text_detec;
import com.example.sba_project.R;
import com.example.sba_project.Userdata.ExtendedMyUserData;
import com.example.sba_project.Userdata.InviteData;
import com.example.sba_project.Userdata.UserDataManager;
import com.example.sba_project.Util.BackPressCloseHandler;
import com.example.sba_project.Util.UtilValues;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;

public class GameRoomActivity extends AppCompatActivity {
    public enum User_Permission {
        HOST(0), CLIENT(1);

        private final int value;

        private User_Permission(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private Spinner gameselc;
    private TextView captain;
    private TextView crew;
    private ImageView master_pro_Image;
    private Button add_team;
    //    private ScrollView teamone_scroll;
    private ListView crewList;
    private Button exit_home;

    private ListView PlayerListView;
    PlayerItem PlayersList = null;

    final ArrayList arrayList = new ArrayList<>(); // 파이널 달린거 주의
    ArrayAdapter<String> arrayAdapter;

    // Request Code
    static final int INVITE = 1000;
    static final int TEXT_DETEC = 1001;

    // Result Code
    static final int INVITE_RESULT_OK = 2000;
    static final int INVITE_RESULT_FAIL = 2001;

    public static final int TEXT_DETEC_RESULT_OK = 3000;
    // Tag
    public static final String USER_DATA = "UserData";
    public static final String ROOM_PERMITION = "Room_Permition";
    public static final String ROOM_NUMBER = "Room_Number";

    private static final int IMAGE_PICK_CAMERA_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 200;

    String cameraPermission[];

    // 디폴트는 Client
    User_Permission userPermission = User_Permission.CLIENT;

    private BackPressCloseHandler backPressCloseHandler;
    Uri image_uri = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case INVITE:
                switch (resultCode) {
                    case INVITE_RESULT_OK:
                        ExtendedMyUserData InvitedUser = (ExtendedMyUserData) data.getSerializableExtra(USER_DATA);
                        InviteMessageToUser(InvitedUser);
                        Toast.makeText(GameRoomActivity.this, "초대 성공!", Toast.LENGTH_SHORT).show();
                        break;
                    case INVITE_RESULT_FAIL:
                        Toast.makeText(GameRoomActivity.this, "초대 실패!", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
            case TEXT_DETEC:
                if(resultCode == TEXT_DETEC_RESULT_OK){
                    String resultStr = data.getStringExtra("Result");
                    UserDataManager.getInstance().getGameRoom().UpdateUserScore(Integer.parseInt(resultStr));
                }
                break;
            case IMAGE_PICK_CAMERA_CODE: {
                if (resultCode == RESULT_OK) {
                    //get drawable bitmap for text recognition
                    CropImage.activity(image_uri)
                            .setGuidelines(CropImageView.Guidelines.ON) //enable image guidlines
                            .start(this);
                }
            }
            break;
            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE: {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    Uri resultUri = result.getUri(); //get image uri

                    Intent intent = new Intent(GameRoomActivity.this, Text_detec.class);
                    intent.putExtra("uri", resultUri);
                    startActivityForResult(intent, TEXT_DETEC);
                }
            }
            break;
        }
    }

    //handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        pickCamera();
                    } else {
                        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    private void pickCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "NewPic"); //title of the picture
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image To text"); //description
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);

        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void InviteMessageToUser(final ExtendedMyUserData InvitedUser) {
        final InviteData inviteData = new InviteData(UserDataManager.getInstance().getGameRoom().getRoom_id(), FirebaseAuth.getInstance().getCurrentUser().getUid(), InvitedUser.uID, InviteData.EInviteFlag.INVITE);
        UtilValues.getInviteRef().push().setValue(inviteData);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_room);

        //camera permission
        cameraPermission = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        SetViews();
        // 게임 룸 컨트롤.
        SetGameRoom();
    }

    private void SetGameRoom() {
        // 이미 게임 룸에 참여했었고, gameroom이 아직 존재하다면..
        final int _roomNum = UserDataManager.getInstance().getRoomNumber();
        if (_roomNum != -1) {
            // 초기화 후 방 살아있는지 확인.
            UserDataManager.getInstance().setRoomNumber(-1);
            FirebaseDatabase.getInstance().getReference().child("GameRoom").equalTo(_roomNum).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    UserDataManager.getInstance().setGameRoom(new GameRoom(GameRoomActivity.this, UserDataManager.getInstance().getCurUserData().NickName, _roomNum, PlayersList));
                    UserDataManager.getInstance().setInGameRoom(true);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    int a = 0;
                }
            });
        } else {
            SetGameRoomResult();
        }
    }

    private void SetGameRoomResult() {
        // 권한 확인
        Intent permit = getIntent();

        if ((User_Permission) permit.getSerializableExtra(GameRoomActivity.ROOM_PERMITION) != null)
            userPermission = (User_Permission) permit.getSerializableExtra(GameRoomActivity.ROOM_PERMITION);

        int RoomNumber = permit.getIntExtra(ROOM_NUMBER, -1);
        PlayersList.setPermission(userPermission);

        // 직접 생성했을 경우와 초대받아서 들어온 경우.
        switch (userPermission) {
            case HOST:
                UserDataManager.getInstance().setGameRoom(new GameRoom(this, UserDataManager.getInstance().getCurUserData().NickName, PlayersList));
                break;
            case CLIENT:
                if (RoomNumber == -1)
                    Toast.makeText(GameRoomActivity.this, "잘못된 방 번호 " + RoomNumber + "입니다.", Toast.LENGTH_SHORT).show();
                else
                    UserDataManager.getInstance().setGameRoom(new GameRoom(this, UserDataManager.getInstance().getCurUserData().NickName, RoomNumber, PlayersList));
                break;
        }
        UserDataManager.getInstance().setInGameRoom(true);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        backPressCloseHandler.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserDataManager.getInstance().getGameRoom().ExitRoom();
    }

    private void SetViews() {
        backPressCloseHandler = new BackPressCloseHandler(this, "\\'뒤로\\'버튼을 한번 더 누르시면 메인 화면으로 돌아갑니다.\"");

        gameselc = (Spinner) findViewById(R.id.gameSelect);
        captain = (TextView) findViewById(R.id.captain);
        crew = (TextView) findViewById(R.id.crew);
        master_pro_Image = (ImageView) findViewById(R.id.profile_image2);
        add_team = (Button) findViewById(R.id.addTeam);
//        teamone_scroll = (ScrollView) findViewById(R.id.scrollView);
//        exit_home = (Button) findViewById(R.id.btn_exit);

        // start button
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UserDataManager.getInstance().getCurGameRoomRef().child("CategoryName").setValue(true);
            }
        });

        // test cam button
        findViewById(R.id.btn_cam).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickCamera();
//                if (!checkCameraPermission()) {
//                    //camera permission not allowed, request it
//                    requestCameraPermission();
//                } else {
//                    //Permission allowed, take picture
//                    pickCamera();
//                }
            }
        });

//        exit_home.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                UserDataManager.getInstance().getGameRoom().ExitRoom();
//            }
//        });

        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, arrayList) {
            @SuppressLint("WrongViewCast")
            public View getView(int positon, View convertView, ViewGroup parent) {
                View v = super.getView(positon, convertView, parent);
                if (positon == getCount()) {
//                    ((TextView)v.findViewById(android.R.id.text1)).setText("");
//                    ((TextView)v.findViewById(android.R.id.text2)).setHint(getItem(getCount()));
                    ((TextView) v.findViewById(R.id.gameSelect)).setText("");
                    ((TextView) v.findViewById(R.id.gameSelect)).setHint(getItem(getCount()));

                }
                return v;
            }

            public int getCount(int getcount) {
                return super.getCount() - 1;
            }
        };
        final DatabaseReference games_ref = FirebaseDatabase.getInstance().getReference("Game").child("title");
        games_ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                arrayList.add("게임선택");
                for (DataSnapshot iter : dataSnapshot.getChildren()) {
                    /*NickName, Address, Age, eMail, PhotoUrl */
                    //ExtendedMyUserData tmpUser = UtilValues.GetUserDataFromDatabase(iter);
                    arrayList.add(iter.getKey());
                }
//                arrayList.add("게임선택");
                arrayAdapter.notifyDataSetChanged();
//                gameselc.setSelection(arrayAdapter.getCount()-1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        gameselc.setPrompt("게임선택");
        gameselc.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                final DatabaseReference tmpRef = UserDataManager.getInstance().getCurGameRoomRef();

                if (tmpRef != null) {
                    tmpRef.child("CategoryName").setValue(arrayList.get(i).toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        gameselc.setAdapter(arrayAdapter);
        gameselc.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (arrayList.get(i).equals("게임선택") == true) {
                    //Toast.makeText(getApplicationContext(),"게임선택해주세요!",Toast.LENGTH_SHORT).show();
                    arrayList.remove(0);
                } else
                    Toast.makeText(getApplicationContext(), arrayList.get(i) + "가 선택되었습니다.",
                            Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        PlayerListView = findViewById(R.id.crewList);

        PlayersList = new PlayerItem();
        PlayersList.SetActivity(this);
        PlayerListView.setAdapter(PlayersList);
        findViewById(R.id.addTeam).setOnClickListener(new Button.OnClickListener() {
                                                          @Override
                                                          public void onClick(View view) {
                                                              Intent intent = new Intent(GameRoomActivity.this, GameRoomPopup.class);
                                                              startActivityForResult(intent, INVITE);
                                                          }
                                                      }
        );

        //exit 버튼

    }
}
