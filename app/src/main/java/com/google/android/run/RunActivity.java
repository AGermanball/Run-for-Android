package com.google.android.run;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class RunActivity extends Activity {

    private EditText inputEditText;
    private TextView closeButton;
    private Button okButton;
    private View runWindow;
    private ImageView iconPlaceholder;  // 使用原来的变量名

    // URL正则表达式
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^((https?|ftp|file)://)?([\\w\\-]+\\.)+[\\w\\-]+(/[\\w\\-./?%&=]*)?$",
        Pattern.CASE_INSENSITIVE
    );

    // 文件路径正则表达式
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile(
        "^(file://)?(/[\\w\\-./]+|/storage/[\\w\\-./]+|/sdcard[\\w\\-./]*)$",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);

        initViews();
        setupListeners();
    }

    private void initViews() {
        inputEditText = findViewById(R.id.input_edittext);
        closeButton = findViewById(R.id.close_button);
        okButton = findViewById(R.id.ok_button);
        runWindow = findViewById(R.id.run_window);
        iconPlaceholder = findViewById(R.id.icon_placeholder);  // 使用原来的ID
    }

    private void setupListeners() {
        // 关闭按钮点击事件
        closeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					hideKeyboard();
					finish();
				}
			});

        // 确定按钮点击事件
        okButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String input = inputEditText.getText().toString().trim();
					if (!TextUtils.isEmpty(input)) {
						processInput(input);
					} else {
						Toast.makeText(RunActivity.this, "请输入内容", Toast.LENGTH_SHORT).show();
					}
				}
			});

        // 输入框点击事件 - 显示键盘
        inputEditText.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showKeyboard();
				}
			});

        // 输入框焦点变化监听
        inputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						// 确保键盘在获得焦点时显示
						showKeyboardWithDelay();
					}
				}
			});

        // 点击输入框外部区域隐藏键盘
        findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 检查点击位置是否在窗口外部
					if (!isPointInsideView(v, runWindow)) {
						hideKeyboard();
					}
				}
			});

        // 点击图标也可以获取输入焦点（可选）
        iconPlaceholder.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					inputEditText.requestFocus();
					showKeyboard();
				}
			});
    }

    // 检查点击位置是否在指定View内
    private boolean isPointInsideView(View view, View targetView) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        int[] targetLocation = new int[2];
        targetView.getLocationOnScreen(targetLocation);
        int targetX = targetLocation[0];
        int targetY = targetLocation[1];
        int targetWidth = targetView.getWidth();
        int targetHeight = targetView.getHeight();

        return x >= targetX && x <= targetX + targetWidth &&
			y >= targetY && y <= targetY + targetHeight;
    }

    private void processInput(String input) {
        hideKeyboard();

        // 1. 检查是否为包名
        if (isPackageName(input)) {
            launchAppByPackageName(input);
            return;
        }

        // 2. 检查是否为URL
        if (isValidUrl(input)) {
            openUrl(input);
            return;
        }

        // 3. 检查是否为文件路径
        if (isFilePath(input)) {
            openFile(input);
            return;
        }

        // 4. 其他情况作为搜索内容
        searchContent(input);
    }

    private boolean isPackageName(String input) {
        // 包名通常包含点号，且不包含空格
        if (input.contains(".") && !input.contains(" ")) {
            try {
                PackageManager pm = getPackageManager();
                pm.getPackageInfo(input, 0);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    private boolean isValidUrl(String input) {
        if (URL_PATTERN.matcher(input).matches()) {
            try {
                // 尝试验证URL格式
                if (!input.startsWith("http://") && !input.startsWith("https://") && 
                    !input.startsWith("ftp://") && !input.startsWith("file://")) {
                    input = "http://" + input;
                }
                new URL(input);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private boolean isFilePath(String input) {
        // 检查是否为file://协议或普通文件路径
        if (input.startsWith("file://") || input.startsWith("/")) {
            return FILE_PATH_PATTERN.matcher(input).matches();
        }
        return false;
    }

    private void launchAppByPackageName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                startActivity(launchIntent);
                finish();
            } else {
                Toast.makeText(this, "无法启动应用: " + packageName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "启动应用失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void openUrl(String url) {
        try {
            // 如果URL没有协议头，添加http://
            if (!url.toLowerCase(Locale.US).startsWith("http://") && 
                !url.toLowerCase(Locale.US).startsWith("https://") &&
                !url.toLowerCase(Locale.US).startsWith("ftp://")) {
                url = "http://" + url;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 验证是否有浏览器可以处理
            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            if (activities.size() > 0) {
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "没有找到浏览器应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "打开URL失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void openFile(String filePath) {
        try {
            // 处理file://协议
            if (filePath.startsWith("file://")) {
                filePath = filePath.substring(7);
            }

            File file = new File(filePath);

            // 检查文件或目录是否存在
            if (!file.exists()) {
                Toast.makeText(this, "文件或目录不存在: " + filePath, Toast.LENGTH_SHORT).show();
                return;
            }

            // 如果是HTML文件，用浏览器打开
            String fileName = file.getName().toLowerCase(Locale.US);
            if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                openUrl("file://" + file.getAbsolutePath());
                return;
            }

            // 使用文件管理器打开
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(file);

            // 根据文件类型设置MIME类型
            if (file.isDirectory()) {
                intent.setDataAndType(uri, "*/*");
            } else {
                // 获取文件扩展名
                String extension = "";
                int i = fileName.lastIndexOf('.');
                if (i > 0) {
                    extension = fileName.substring(i + 1);
                }

                // 设置MIME类型
                String mimeType = getMimeType(extension);
                intent.setDataAndType(uri, mimeType);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 验证是否有应用可以处理
            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            if (activities.size() > 0) {
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "没有找到可以打开此文件的应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "打开文件失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String getMimeType(String extension) {
        switch (extension.toLowerCase(Locale.US)) {
            case "txt": return "text/plain";
            case "pdf": return "application/pdf";
            case "doc": case "docx": return "application/msword";
            case "xls": case "xlsx": return "application/vnd.ms-excel";
            case "ppt": case "pptx": return "application/vnd.ms-powerpoint";
            case "jpg": case "jpeg": case "png": case "gif": case "bmp": 
                return "image/*";
            case "mp3": case "wav": case "ogg": return "audio/*";
            case "mp4": case "avi": case "mkv": case "mov": return "video/*";
            default: return "*/*";
        }
    }

    private void searchContent(String query) {
        try {
            String searchUrl = "https://www.google.com/search?q=" + Uri.encode(query);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            if (activities.size() > 0) {
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "没有找到浏览器应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "搜索失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // 显示键盘
    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && inputEditText != null) {
            inputEditText.requestFocus();
            imm.showSoftInput(inputEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // 延迟显示键盘（用于焦点变化时）
    private void showKeyboardWithDelay() {
        inputEditText.postDelayed(new Runnable() {
				@Override
				public void run() {
					showKeyboard();
				}
			}, 100);
    }

    // 隐藏键盘
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && inputEditText != null) {
            imm.hideSoftInputFromWindow(inputEditText.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        hideKeyboard();
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard();
    }
}
