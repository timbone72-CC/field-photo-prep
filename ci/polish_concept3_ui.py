#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
main_path = root / "app/src/main/java/com/inandout/fieldphotoprep/MainActivity.java"
photo_path = root / "app/src/main/java/com/inandout/fieldphotoprep/PhotoCaptureActivity.java"

main = main_path.read_text(encoding="utf-8")
if "private void setStatusText(String message)" not in main:
    main = main.replace("statusText.setText(", "setStatusText(")
    marker = "    private void showMessage(String message) {"
    helper = '''    private void setStatusText(String message) {\n        if (statusText == null) {\n            return;\n        }\n        statusText.setText(message == null ? \"\" : message);\n        if (screen == Screen.WORK_ORDERS) {\n            statusText.setVisibility(message == null || message.isBlank() ? View.GONE : View.VISIBLE);\n        }\n    }\n\n'''
    main = main.replace(marker, helper + marker, 1)
main = main.replace(
    "controller.setAppearanceLightStatusBars(true);\n            controller.setAppearanceLightNavigationBars(true);\n            getWindow().setStatusBarColor(Color.WHITE);\n            getWindow().setNavigationBarColor(Color.WHITE);",
    "controller.setAppearanceLightStatusBars(!night);\n            controller.setAppearanceLightNavigationBars(!night);\n            int color = ContextCompat.getColor(this, R.color.home_background);\n            getWindow().setStatusBarColor(color);\n            getWindow().setNavigationBarColor(color);")
main_path.write_text(main, encoding="utf-8")

photo = photo_path.read_text(encoding="utf-8")
if "import android.content.res.Configuration;" not in photo:
    photo = photo.replace("import android.content.Intent;\n", "import android.content.Intent;\nimport android.content.res.Configuration;\n")
photo = photo.replace(
    "controller.setAppearanceLightStatusBars(true);\n    controller.setAppearanceLightNavigationBars(true);",
    "boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)\n            == Configuration.UI_MODE_NIGHT_YES;\n    controller.setAppearanceLightStatusBars(!night);\n    controller.setAppearanceLightNavigationBars(!night);")
photo = photo.replace(
    'showPhotoStatus("Ready for photos. Use the Send boxes to build a batch or tap a photo for individual details.");',
    "showPhotoStatus(null);")
photo_path.write_text(photo, encoding="utf-8")

print("Concept 3 final polish applied")
