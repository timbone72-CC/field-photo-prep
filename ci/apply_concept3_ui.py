#!/usr/bin/env python3
from __future__ import annotations

import pathlib
import subprocess
import textwrap

ROOT = pathlib.Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/java/com/inandout/fieldphotoprep/MainActivity.java"
PHOTO = ROOT / "app/src/main/java/com/inandout/fieldphotoprep/PhotoCaptureActivity.java"
RES = ROOT / "app/src/main/res"


def run(*args: str) -> None:
    subprocess.run(args, cwd=ROOT, check=True)


def write(path: pathlib.Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(textwrap.dedent(content).lstrip(), encoding="utf-8")


def replace_between(text: str, start: str, end: str, replacement: str) -> str:
    a = text.index(start)
    b = text.index(end, a)
    return text[:a] + textwrap.dedent(replacement).lstrip() + "\n\n    " + text[b:]


def add_after(text: str, needle: str, addition: str) -> str:
    if addition.strip() in text:
        return text
    return text.replace(needle, needle + addition, 1)


# Reuse only the useful, already-tested Phase 9A Home plumbing, then replace its visual direction.
run("git", "fetch", "origin", "feat/phase-9a-home-properties")
run(
    "git", "checkout", "origin/feat/phase-9a-home-properties", "--",
    "app/src/main/java/com/inandout/fieldphotoprep/MainActivity.java",
    "app/src/main/java/com/inandout/fieldphotoprep/PropertyDisplayName.java",
    "app/src/main/java/com/inandout/fieldphotoprep/PropertyListAdapter.java",
    "app/src/test/java/com/inandout/fieldphotoprep/PropertyDisplayNameTest.java",
)

# ---------- Shared Concept 3 resources ----------
write(RES / "values/colors.xml", r'''
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="home_background">#F5F8F6</color>
    <color name="home_surface">#FFFFFF</color>
    <color name="home_surface_alt">#EEF4F0</color>
    <color name="home_text_primary">#17211B</color>
    <color name="home_text_secondary">#66736B</color>
    <color name="home_primary">#0B8B5C</color>
    <color name="home_primary_dark">#076D49</color>
    <color name="home_primary_soft">#E3F4EC</color>
    <color name="home_blue">#1473E6</color>
    <color name="home_blue_dark">#0D5CBD</color>
    <color name="home_blue_soft">#E9F2FF</color>
    <color name="home_on_primary">#FFFFFF</color>
    <color name="home_outline">#D8E0DA</color>
    <color name="home_error">#B42318</color>
    <color name="home_error_container">#FDECEA</color>
    <color name="home_warning">#B35C00</color>
    <color name="home_warning_container">#FFF3E0</color>
</resources>
''')

write(RES / "values-night/colors.xml", r'''
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="home_background">#101512</color>
    <color name="home_surface">#18201B</color>
    <color name="home_surface_alt">#202B24</color>
    <color name="home_text_primary">#F2F7F3</color>
    <color name="home_text_secondary">#B2BDB5</color>
    <color name="home_primary">#55D99D</color>
    <color name="home_primary_dark">#2FBA7C</color>
    <color name="home_primary_soft">#173A2B</color>
    <color name="home_blue">#78B2FF</color>
    <color name="home_blue_dark">#4E93EC</color>
    <color name="home_blue_soft">#162B45</color>
    <color name="home_on_primary">#07130D</color>
    <color name="home_outline">#34423A</color>
    <color name="home_error">#FFB4AB</color>
    <color name="home_error_container">#4A1F1C</color>
    <color name="home_warning">#FFB870</color>
    <color name="home_warning_container">#4A2B10</color>
</resources>
''')

for name, body in {
    "bg_concept_card.xml": '''
        <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
            <solid android:color="@color/home_surface" />
            <stroke android:width="1dp" android:color="@color/home_outline" />
            <corners android:radius="16dp" />
        </shape>''',
    "bg_concept_green_card.xml": '''
        <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
            <gradient android:startColor="@color/home_primary" android:endColor="@color/home_primary_dark" android:angle="0" />
            <corners android:radius="18dp" />
        </shape>''',
    "bg_concept_blue_button.xml": '''
        <selector xmlns:android="http://schemas.android.com/apk/res/android">
            <item android:state_pressed="true"><shape><solid android:color="@color/home_blue_dark"/><corners android:radius="14dp"/></shape></item>
            <item><shape><solid android:color="@color/home_blue"/><corners android:radius="14dp"/></shape></item>
        </selector>''',
    "bg_concept_green_button.xml": '''
        <selector xmlns:android="http://schemas.android.com/apk/res/android">
            <item android:state_pressed="true"><shape><solid android:color="@color/home_primary_dark"/><corners android:radius="14dp"/></shape></item>
            <item><shape><solid android:color="@color/home_primary"/><corners android:radius="14dp"/></shape></item>
        </selector>''',
    "bg_concept_soft_green.xml": '''
        <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
            <solid android:color="@color/home_primary_soft" />
            <corners android:radius="999dp" />
        </shape>''',
    "bg_concept_soft_blue.xml": '''
        <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
            <solid android:color="@color/home_blue_soft" />
            <corners android:radius="999dp" />
        </shape>''',
    "bg_concept_nav.xml": '''
        <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
            <solid android:color="@color/home_surface" />
            <stroke android:width="1dp" android:color="@color/home_outline" />
        </shape>''',
    "bg_concept_selected.xml": '''
        <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
            <solid android:color="@color/home_primary_soft" />
            <stroke android:width="2dp" android:color="@color/home_primary" />
            <corners android:radius="14dp" />
        </shape>''',
    "bg_concept_status.xml": '''
        <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
            <solid android:color="@color/home_error_container" />
            <corners android:radius="12dp" />
        </shape>''',
}.items():
    write(RES / "drawable" / name, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + body)

# ---------- Home ----------
write(RES / "layout/screen_home_properties.xml", r'''
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/home_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/home_background">

    <LinearLayout
        android:id="@+id/home_content"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:paddingStart="14dp"
        android:paddingEnd="14dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Field Photo Prep"
                android:textColor="@color/home_text_primary"
                android:textSize="20sp"
                android:textStyle="bold" />

            <ImageButton
                android:id="@+id/home_drive_options_button"
                android:layout_width="48dp"
                android:layout_height="48dp"
                android:background="?android:attr/selectableItemBackgroundBorderless"
                android:contentDescription="Storage options"
                android:padding="12dp"
                android:src="@drawable/ic_home_more_24" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/drive_status_strip"
            android:layout_width="match_parent"
            android:layout_height="78dp"
            android:background="@drawable/bg_concept_green_card"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:paddingStart="16dp"
            android:paddingEnd="8dp">

            <View
                android:id="@+id/drive_status_dot"
                android:layout_width="12dp"
                android:layout_height="12dp"
                android:layout_marginEnd="12dp"
                android:background="@drawable/bg_home_status_dot" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/home_master_name"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:ellipsize="end"
                    android:maxLines="1"
                    android:text="Google Drive"
                    android:textColor="@color/home_on_primary"
                    android:textSize="16sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/home_drive_state"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="2dp"
                    android:ellipsize="end"
                    android:maxLines="1"
                    android:text="Not connected"
                    android:textColor="#E8FFF2"
                    android:textSize="12sp" />
            </LinearLayout>

            <ProgressBar
                android:id="@+id/home_progress"
                style="?android:attr/progressBarStyleSmall"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:padding="9dp"
                android:visibility="gone" />

            <ImageButton
                android:id="@+id/home_refresh_button"
                android:layout_width="48dp"
                android:layout_height="48dp"
                android:background="?android:attr/selectableItemBackgroundBorderless"
                android:contentDescription="Refresh properties"
                android:padding="12dp"
                android:src="@drawable/ic_home_refresh_24" />

            <Button
                android:id="@+id/home_connect_button"
                android:layout_width="wrap_content"
                android:layout_height="44dp"
                android:minWidth="0dp"
                android:paddingStart="12dp"
                android:paddingEnd="12dp"
                android:text="Connect"
                android:textAllCaps="false"
                android:visibility="gone" />
        </LinearLayout>

        <TextView
            android:id="@+id/home_status_text"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:background="@drawable/bg_concept_status"
            android:padding="10dp"
            android:textColor="@color/home_error"
            android:textSize="12sp"
            android:visibility="gone" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="58dp"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Properties"
                    android:textColor="@color/home_text_primary"
                    android:textSize="17sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/home_property_count"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="1dp"
                    android:text="0 properties"
                    android:textColor="@color/home_text_secondary"
                    android:textSize="12sp" />
            </LinearLayout>

            <Button
                android:id="@+id/home_new_address_button"
                android:layout_width="wrap_content"
                android:layout_height="44dp"
                android:background="@drawable/bg_concept_blue_button"
                android:minWidth="0dp"
                android:paddingStart="14dp"
                android:paddingEnd="14dp"
                android:text="+ New Address"
                android:textAllCaps="false"
                android:textColor="@android:color/white"
                android:textSize="13sp"
                android:textStyle="bold" />
        </LinearLayout>

        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1">

            <ListView
                android:id="@+id/home_property_list"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:clipToPadding="false"
                android:divider="@android:color/transparent"
                android:dividerHeight="7dp"
                android:paddingBottom="8dp"
                android:scrollbars="vertical" />

            <TextView
                android:id="@+id/home_empty_text"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:padding="24dp"
                android:text="No properties yet"
                android:textColor="@color/home_text_secondary"
                android:textSize="14sp"
                android:visibility="gone" />
        </FrameLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="64dp"
            android:background="@drawable/bg_concept_nav"
            android:gravity="center"
            android:orientation="horizontal">

            <Button
                android:id="@+id/home_nav_home"
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:background="@android:color/transparent"
                android:text="Home"
                android:textAllCaps="false"
                android:textColor="@color/home_primary"
                android:textStyle="bold" />

            <Button
                android:id="@+id/home_nav_work_orders"
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:background="@android:color/transparent"
                android:text="Work Orders"
                android:textAllCaps="false"
                android:textColor="@color/home_text_secondary" />

            <Button
                android:id="@+id/home_nav_photos"
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:background="@android:color/transparent"
                android:text="Photos"
                android:textAllCaps="false"
                android:textColor="@color/home_text_secondary" />
        </LinearLayout>
    </LinearLayout>
</FrameLayout>
''')

write(RES / "layout/row_home_property.xml", r'''
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/property_row"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minHeight="68dp"
    android:background="@drawable/bg_concept_card"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingStart="14dp"
    android:paddingTop="8dp"
    android:paddingEnd="8dp"
    android:paddingBottom="8dp">

    <TextView
        android:layout_width="38dp"
        android:layout_height="38dp"
        android:background="@drawable/bg_concept_soft_green"
        android:gravity="center"
        android:text="⌂"
        android:textColor="@color/home_primary"
        android:textSize="19sp"
        android:textStyle="bold" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="12dp"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/property_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:ellipsize="end"
            android:maxLines="2"
            android:textColor="@color/home_text_primary"
            android:textSize="15sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/property_disambiguator"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:maxLines="1"
            android:textColor="@color/home_text_secondary"
            android:textSize="11sp"
            android:visibility="gone" />
    </LinearLayout>

    <ImageView
        android:layout_width="40dp"
        android:layout_height="48dp"
        android:contentDescription="Open property"
        android:padding="8dp"
        android:src="@drawable/ic_home_chevron_24" />
</LinearLayout>
''')

# ---------- Work Orders ----------
write(RES / "layout/screen_work_orders.xml", r'''
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/work_orders_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/home_background"
    android:orientation="vertical"
    android:paddingStart="14dp"
    android:paddingEnd="14dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:gravity="center_vertical"
        android:orientation="horizontal">
        <Button
            android:id="@+id/work_order_back"
            android:layout_width="44dp"
            android:layout_height="44dp"
            android:background="@android:color/transparent"
            android:minWidth="0dp"
            android:text="‹"
            android:textAllCaps="false"
            android:textColor="@color/home_text_primary"
            android:textSize="28sp" />
        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Work Orders"
            android:textColor="@color/home_text_primary"
            android:textSize="20sp"
            android:textStyle="bold" />
        <Button
            android:id="@+id/work_order_refresh"
            android:layout_width="48dp"
            android:layout_height="44dp"
            android:background="@android:color/transparent"
            android:minWidth="0dp"
            android:text="↻"
            android:textAllCaps="false"
            android:textColor="@color/home_text_primary"
            android:textSize="20sp" />
    </LinearLayout>

    <TextView android:id="@+id/work_order_master_text" android:layout_width="1dp" android:layout_height="1dp" android:visibility="gone" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@drawable/bg_concept_card"
        android:orientation="vertical"
        android:padding="14dp">
        <TextView
            android:id="@+id/work_order_address"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:ellipsize="end"
            android:maxLines="2"
            android:textColor="@color/home_text_primary"
            android:textSize="16sp"
            android:textStyle="bold" />
        <TextView
            android:id="@+id/work_order_current"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:text="No work order selected"
            android:textColor="@color/home_text_secondary"
            android:textSize="12sp" />
        <Button
            android:id="@+id/work_order_photos"
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:layout_marginTop="10dp"
            android:background="@drawable/bg_concept_blue_button"
            android:text="Open Photos"
            android:textAllCaps="false"
            android:textColor="@android:color/white"
            android:textStyle="bold" />
    </LinearLayout>

    <TextView
        android:id="@+id/work_order_status"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:background="@drawable/bg_concept_status"
        android:padding="9dp"
        android:textColor="@color/home_error"
        android:textSize="12sp"
        android:visibility="gone" />

    <ScrollView
        android:id="@+id/work_order_scroll"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:fillViewport="true">
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingTop="10dp"
            android:paddingBottom="10dp">

            <TextView
                android:id="@+id/work_order_list_heading"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Work Orders"
                android:textColor="@color/home_text_primary"
                android:textSize="16sp"
                android:textStyle="bold" />

            <ListView
                android:id="@+id/work_order_list"
                android:layout_width="match_parent"
                android:layout_height="260dp"
                android:layout_marginTop="6dp"
                android:divider="@android:color/transparent"
                android:dividerHeight="7dp"
                android:nestedScrollingEnabled="false" />

            <Button
                android:id="@+id/work_order_select_existing"
                android:layout_width="1dp"
                android:layout_height="1dp"
                android:visibility="gone" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:text="New dated work order"
                android:textColor="@color/home_text_primary"
                android:textSize="16sp"
                android:textStyle="bold" />

            <EditText
                android:id="@+id/work_order_name_input"
                android:layout_width="match_parent"
                android:layout_height="52dp"
                android:layout_marginTop="6dp"
                android:background="@drawable/bg_concept_card"
                android:hint="Work order name"
                android:paddingStart="14dp"
                android:paddingEnd="14dp"
                android:singleLine="true"
                android:textColor="@color/home_text_primary"
                android:textColorHint="@color/home_text_secondary" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:orientation="horizontal">
                <Button
                    android:id="@+id/work_order_date_button"
                    android:layout_width="0dp"
                    android:layout_height="48dp"
                    android:layout_weight="1"
                    android:textAllCaps="false" />
                <Button
                    android:id="@+id/work_order_create_button"
                    android:layout_width="0dp"
                    android:layout_height="48dp"
                    android:layout_marginStart="8dp"
                    android:layout_weight="1"
                    android:background="@drawable/bg_concept_blue_button"
                    android:text="Add Work Order"
                    android:textAllCaps="false"
                    android:textColor="@android:color/white"
                    android:textStyle="bold" />
            </LinearLayout>

            <Button
                android:id="@+id/work_order_maintenance_toggle"
                android:layout_width="match_parent"
                android:layout_height="46dp"
                android:layout_marginTop="10dp"
                android:text="Maintenance actions"
                android:textAllCaps="false" />

            <LinearLayout
                android:id="@+id/work_order_maintenance"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:visibility="gone">
                <Button
                    android:id="@+id/work_order_reuse_empty"
                    android:layout_width="match_parent"
                    android:layout_height="48dp"
                    android:text="Reuse Selected Empty Folder"
                    android:textAllCaps="false" />
                <Button
                    android:id="@+id/work_order_clear_reuse"
                    android:layout_width="match_parent"
                    android:layout_height="48dp"
                    android:text="Clear &amp; Reuse Selected Folder"
                    android:textAllCaps="false" />
            </LinearLayout>
        </LinearLayout>
    </ScrollView>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="64dp"
        android:background="@drawable/bg_concept_nav"
        android:gravity="center"
        android:orientation="horizontal">
        <Button android:id="@+id/work_nav_home" android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" android:background="@android:color/transparent" android:text="Home" android:textAllCaps="false" android:textColor="@color/home_text_secondary" />
        <Button android:id="@+id/work_nav_work_orders" android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" android:background="@android:color/transparent" android:text="Work Orders" android:textAllCaps="false" android:textColor="@color/home_primary" android:textStyle="bold" />
        <Button android:id="@+id/work_nav_photos" android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" android:background="@android:color/transparent" android:text="Photos" android:textAllCaps="false" android:textColor="@color/home_text_secondary" />
    </LinearLayout>
</LinearLayout>
''')

write(RES / "layout/row_work_order.xml", r'''
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/work_order_row"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minHeight="72dp"
    android:background="@drawable/bg_concept_card"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingStart="12dp"
    android:paddingTop="9dp"
    android:paddingEnd="8dp"
    android:paddingBottom="9dp">
    <TextView android:layout_width="40dp" android:layout_height="40dp" android:background="@drawable/bg_concept_soft_blue" android:gravity="center" android:text="▣" android:textColor="@color/home_blue" android:textSize="18sp" />
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="10dp" android:layout_weight="1" android:orientation="vertical">
        <TextView android:id="@+id/work_order_row_title" android:layout_width="match_parent" android:layout_height="wrap_content" android:ellipsize="end" android:maxLines="1" android:textColor="@color/home_text_primary" android:textSize="15sp" android:textStyle="bold" />
        <TextView android:id="@+id/work_order_row_date" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:textColor="@color/home_text_secondary" android:textSize="12sp" />
    </LinearLayout>
    <TextView android:id="@+id/work_order_row_state" android:layout_width="wrap_content" android:layout_height="32dp" android:background="@drawable/bg_concept_soft_green" android:gravity="center" android:paddingStart="10dp" android:paddingEnd="10dp" android:text="Selected" android:textColor="@color/home_primary" android:textSize="11sp" android:visibility="gone" />
    <ImageView android:layout_width="36dp" android:layout_height="44dp" android:contentDescription="Select work order" android:padding="8dp" android:src="@drawable/ic_home_chevron_24" />
</LinearLayout>
''')

write(ROOT / "app/src/main/java/com/inandout/fieldphotoprep/WorkOrderListAdapter.java", r'''
package com.inandout.fieldphotoprep;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

final class WorkOrderListAdapter extends ArrayAdapter<DriveFolder> {
    private final LayoutInflater inflater;
    private String selectedId;

    WorkOrderListAdapter(Context context, List<DriveFolder> folders) {
        super(context, R.layout.row_work_order, folders);
        inflater = LayoutInflater.from(context);
    }

    void setSelectedId(String selectedId) {
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView == null
                ? inflater.inflate(R.layout.row_work_order, parent, false)
                : convertView;
        DriveFolder folder = getItem(position);
        TextView title = row.findViewById(R.id.work_order_row_title);
        TextView date = row.findViewById(R.id.work_order_row_date);
        TextView state = row.findViewById(R.id.work_order_row_state);
        if (folder == null) {
            title.setText("");
            date.setText("");
            state.setVisibility(View.GONE);
            return row;
        }
        String name = PropertyDisplayName.fromDriveFolderName(folder.name());
        int split = name.lastIndexOf(" - ");
        if (split > 0 && split + 3 < name.length()) {
            title.setText(name.substring(0, split));
            date.setText(name.substring(split + 3));
        } else {
            title.setText(name);
            date.setText("Existing work order");
        }
        boolean selected = selectedId != null && selectedId.equals(folder.id());
        row.setBackgroundResource(selected ? R.drawable.bg_concept_selected : R.drawable.bg_concept_card);
        state.setVisibility(selected ? View.VISIBLE : View.GONE);
        return row;
    }
}
''')

# ---------- Photos ----------
write(RES / "layout/screen_photos.xml", r'''
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/photos_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/home_background">
    <LinearLayout
        android:id="@+id/photos_content"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:paddingStart="14dp"
        android:paddingEnd="14dp">
        <LinearLayout android:layout_width="match_parent" android:layout_height="56dp" android:gravity="center_vertical" android:orientation="horizontal">
            <Button android:id="@+id/photos_back" android:layout_width="44dp" android:layout_height="44dp" android:background="@android:color/transparent" android:minWidth="0dp" android:text="‹" android:textAllCaps="false" android:textColor="@color/home_text_primary" android:textSize="28sp" />
            <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="Photos" android:textColor="@color/home_text_primary" android:textSize="20sp" android:textStyle="bold" />
        </LinearLayout>

        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_concept_card" android:orientation="vertical" android:padding="12dp">
            <TextView android:id="@+id/photos_address" android:layout_width="match_parent" android:layout_height="wrap_content" android:ellipsize="end" android:maxLines="1" android:textColor="@color/home_text_primary" android:textSize="15sp" android:textStyle="bold" />
            <TextView android:id="@+id/photos_work_order" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:ellipsize="end" android:maxLines="1" android:textColor="@color/home_text_secondary" android:textSize="12sp" />
        </LinearLayout>

        <TextView android:id="@+id/photos_status" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:background="@drawable/bg_concept_status" android:padding="9dp" android:textColor="@color/home_error" android:textSize="12sp" android:visibility="gone" />

        <Button android:id="@+id/photos_open_camera" android:layout_width="match_parent" android:layout_height="52dp" android:layout_marginTop="10dp" android:background="@drawable/bg_concept_blue_button" android:text="▣  Open Camera" android:textAllCaps="false" android:textColor="@android:color/white" android:textSize="15sp" android:textStyle="bold" />

        <LinearLayout android:layout_width="match_parent" android:layout_height="48dp" android:gravity="center_vertical" android:orientation="horizontal">
            <TextView android:id="@+id/photos_pending_count" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="Photos" android:textColor="@color/home_text_primary" android:textSize="15sp" android:textStyle="bold" />
            <Button android:id="@+id/photos_select_all" android:layout_width="wrap_content" android:layout_height="40dp" android:minWidth="0dp" android:text="Select ready" android:textAllCaps="false" android:textSize="12sp" />
            <Button android:id="@+id/photos_clear_selection" android:layout_width="wrap_content" android:layout_height="40dp" android:minWidth="0dp" android:text="Clear" android:textAllCaps="false" android:textSize="12sp" />
        </LinearLayout>
        <TextView android:id="@+id/photos_batch_selection" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingBottom="5dp" android:textColor="@color/home_text_secondary" android:textSize="12sp" />

        <ScrollView android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:fillViewport="true">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingBottom="8dp">
                <LinearLayout android:id="@+id/photos_pending_list" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" />
                <LinearLayout android:id="@+id/photos_selected_panel" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:background="@drawable/bg_concept_card" android:orientation="vertical" android:padding="12dp" android:visibility="gone">
                    <TextView android:id="@+id/photos_selected_text" android:layout_width="match_parent" android:layout_height="wrap_content" android:textColor="@color/home_text_primary" android:textSize="13sp" android:textStyle="bold" />
                    <TextView android:id="@+id/photos_prepared_text" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="3dp" android:textColor="@color/home_text_secondary" android:textSize="12sp" />
                    <Button android:id="@+id/photos_prepare" android:layout_width="match_parent" android:layout_height="44dp" android:layout_marginTop="6dp" android:text="Prepare selected photo" android:textAllCaps="false" />
                    <Button android:id="@+id/photos_upload_one" android:layout_width="match_parent" android:layout_height="44dp" android:text="Upload this photo" android:textAllCaps="false" />
                    <Button android:id="@+id/photos_reconcile" android:layout_width="match_parent" android:layout_height="44dp" android:text="Reconcile uncertain upload" android:textAllCaps="false" />
                    <Button android:id="@+id/photos_discard" android:layout_width="match_parent" android:layout_height="44dp" android:text="Discard temporary photo" android:textAllCaps="false" />
                </LinearLayout>
            </LinearLayout>
        </ScrollView>

        <Button android:id="@+id/photos_upload_selected" android:layout_width="match_parent" android:layout_height="52dp" android:background="@drawable/bg_concept_green_button" android:text="Upload Selected (0)" android:textAllCaps="false" android:textColor="@android:color/white" android:textSize="15sp" android:textStyle="bold" />
        <LinearLayout android:layout_width="match_parent" android:layout_height="60dp" android:background="@drawable/bg_concept_nav" android:gravity="center" android:orientation="horizontal">
            <Button android:id="@+id/photos_nav_home" android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" android:background="@android:color/transparent" android:text="Home" android:textAllCaps="false" android:textColor="@color/home_text_secondary" />
            <Button android:id="@+id/photos_nav_work_orders" android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" android:background="@android:color/transparent" android:text="Work Orders" android:textAllCaps="false" android:textColor="@color/home_text_secondary" />
            <Button android:id="@+id/photos_nav_photos" android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" android:background="@android:color/transparent" android:text="Photos" android:textAllCaps="false" android:textColor="@color/home_primary" android:textStyle="bold" />
        </LinearLayout>
    </LinearLayout>
</FrameLayout>
''')

write(RES / "layout/row_photo.xml", r'''
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/photo_row_root"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="7dp"
    android:minHeight="76dp"
    android:background="@drawable/bg_concept_card"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingStart="8dp"
    android:paddingTop="7dp"
    android:paddingEnd="10dp"
    android:paddingBottom="7dp">
    <CheckBox android:id="@+id/photo_row_check" android:layout_width="44dp" android:layout_height="48dp" android:buttonTint="@color/home_primary" android:contentDescription="Select photo for upload" />
    <ImageView android:id="@+id/photo_row_thumb" android:layout_width="62dp" android:layout_height="62dp" android:background="@color/home_surface_alt" android:contentDescription="Photo thumbnail" android:scaleType="centerCrop" />
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="10dp" android:layout_weight="1" android:orientation="vertical">
        <TextView android:id="@+id/photo_row_status" android:layout_width="match_parent" android:layout_height="wrap_content" android:textColor="@color/home_text_primary" android:textSize="13sp" android:textStyle="bold" />
        <TextView android:id="@+id/photo_row_time" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="3dp" android:textColor="@color/home_text_secondary" android:textSize="11sp" />
    </LinearLayout>
    <TextView android:layout_width="32dp" android:layout_height="40dp" android:gravity="center" android:text="⋯" android:textColor="@color/home_text_secondary" android:textSize="22sp" />
</LinearLayout>
''')

# ---------- Patch imported Phase 9A MainActivity ----------
main = MAIN.read_text(encoding="utf-8")
main = add_after(main, "    private Button photosButton;\n", "    private ListView workOrderList;\n    private WorkOrderListAdapter workOrderAdapter;\n    private LinearLayout maintenanceControls;\n    private Button maintenanceButton;\n    private Button homeNavWorkOrdersButton;\n    private Button homeNavPhotosButton;\n    private Button workNavHomeButton;\n    private Button workNavPhotosButton;\n")

home_ui_start = "    private void buildHomeUi() {"
home_ui_end = "    private void showDriveOptions(View anchor) {"
main = replace_between(main, home_ui_start, home_ui_end, r'''
    private void buildHomeUi() {
        homeRoot = LayoutInflater.from(this).inflate(R.layout.screen_home_properties, appRoot, false);
        homeStatusText = homeRoot.findViewById(R.id.home_status_text);
        homeMasterNameText = homeRoot.findViewById(R.id.home_master_name);
        homeDriveStateText = homeRoot.findViewById(R.id.home_drive_state);
        homePropertyCountText = homeRoot.findViewById(R.id.home_property_count);
        homeEmptyText = homeRoot.findViewById(R.id.home_empty_text);
        homeDriveStatusDot = homeRoot.findViewById(R.id.drive_status_dot);
        homeProgress = homeRoot.findViewById(R.id.home_progress);
        driveOptionsButton = homeRoot.findViewById(R.id.home_drive_options_button);

        chooseMasterButton = homeRoot.findViewById(R.id.home_connect_button);
        refreshAddressButton = homeRoot.findViewById(R.id.home_refresh_button);
        useCreateAddressButton = homeRoot.findViewById(R.id.home_new_address_button);
        folderList = homeRoot.findViewById(R.id.home_property_list);
        homeNavWorkOrdersButton = homeRoot.findViewById(R.id.home_nav_work_orders);
        homeNavPhotosButton = homeRoot.findViewById(R.id.home_nav_photos);

        chooseMasterButton.setOnClickListener(v -> chooseMasterFolder());
        refreshAddressButton.setOnClickListener(v -> refreshAddressFolders());
        useCreateAddressButton.setOnClickListener(v -> showAddressEntryDialog());
        driveOptionsButton.setOnClickListener(this::showDriveOptions);
        homeNavWorkOrdersButton.setOnClickListener(v -> openSavedPropertyFromHome());
        homeNavPhotosButton.setOnClickListener(v -> openSavedPhotosFromHome());

        adapter = new PropertyListAdapter(this, visibleFolders);
        folderList.setAdapter(adapter);
        folderList.setOnItemClickListener((parent, view, position, id) -> {
            if (screen != Screen.ADDRESSES || busy || position < 0 || position >= visibleFolders.size()) {
                return;
            }
            openAddress(visibleFolders.get(position));
        });

        ViewCompat.setOnApplyWindowInsetsListener(homeRoot, (view, insets) -> {
            var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(homeRoot);
    }

    private void openSavedPropertyFromHome() {
        DriveFolder saved = folderPrefs.getCurrentAddress();
        if (saved == null) {
            showHomeInlineMessage("Choose a property first.");
            return;
        }
        DriveFolder actual = DriveClient.findById(visibleFolders, saved.id());
        if (actual == null) {
            showHomeInlineMessage("Refresh Properties, then choose the property you want to open.");
            return;
        }
        openAddress(actual);
    }

    private void openSavedPhotosFromHome() {
        DriveFolder savedAddress = folderPrefs.getCurrentAddress();
        DriveFolder savedWorkOrder = folderPrefs.getCurrentWorkOrder();
        if (savedAddress == null || savedWorkOrder == null) {
            showHomeInlineMessage("Choose a property and work order before opening Photos.");
            return;
        }
        startActivity(new Intent(this, PhotoCaptureActivity.class));
    }
''')

main = replace_between(main, "    private void buildLegacyWorkOrderUi() {", "    private void chooseMasterFolder() {", r'''
    private void buildLegacyWorkOrderUi() {
        legacyRoot = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.screen_work_orders, appRoot, false);
        legacyStatusText = legacyRoot.findViewById(R.id.work_order_status);
        legacyMasterText = legacyRoot.findViewById(R.id.work_order_master_text);
        addressText = legacyRoot.findViewById(R.id.work_order_address);
        currentWorkOrderText = legacyRoot.findViewById(R.id.work_order_current);
        workOrderScroll = legacyRoot.findViewById(R.id.work_order_scroll);
        workOrderInput = legacyRoot.findViewById(R.id.work_order_name_input);
        backButton = legacyRoot.findViewById(R.id.work_order_back);
        refreshWorkOrdersButton = legacyRoot.findViewById(R.id.work_order_refresh);
        selectWorkOrderButton = legacyRoot.findViewById(R.id.work_order_select_existing);
        dateButton = legacyRoot.findViewById(R.id.work_order_date_button);
        useCreateButton = legacyRoot.findViewById(R.id.work_order_create_button);
        reuseEmptyButton = legacyRoot.findViewById(R.id.work_order_reuse_empty);
        clearReuseButton = legacyRoot.findViewById(R.id.work_order_clear_reuse);
        photosButton = legacyRoot.findViewById(R.id.work_order_photos);
        workOrderList = legacyRoot.findViewById(R.id.work_order_list);
        maintenanceControls = legacyRoot.findViewById(R.id.work_order_maintenance);
        maintenanceButton = legacyRoot.findViewById(R.id.work_order_maintenance_toggle);
        workNavHomeButton = legacyRoot.findViewById(R.id.work_nav_home);
        workNavPhotosButton = legacyRoot.findViewById(R.id.work_nav_photos);

        workOrderControls = (LinearLayout) workOrderScroll.getChildAt(0);
        workOrderAdapter = new WorkOrderListAdapter(this, visibleFolders);
        workOrderList.setAdapter(workOrderAdapter);
        workOrderList.setOnItemClickListener((parent, view, position, id) -> {
            if (busy || position < 0 || position >= visibleFolders.size()) {
                return;
            }
            selectWorkOrder(visibleFolders.get(position), "Work order selected");
        });

        backButton.setOnClickListener(v -> showAddressScreen(true));
        refreshWorkOrdersButton.setOnClickListener(v -> refreshWorkOrderFolders());
        selectWorkOrderButton.setOnClickListener(v -> showWorkOrderPicker());
        dateButton.setOnClickListener(v -> chooseWorkOrderDate());
        useCreateButton.setOnClickListener(v -> useOrCreateWorkOrder());
        reuseEmptyButton.setOnClickListener(v -> reuseSelectedEmptyFolder());
        clearReuseButton.setOnClickListener(v -> prepareClearAndReuse());
        photosButton.setOnClickListener(v -> openPhotoCapture());
        maintenanceButton.setOnClickListener(v -> maintenanceControls.setVisibility(
                maintenanceControls.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        workNavHomeButton.setOnClickListener(v -> showAddressScreen(true));
        workNavPhotosButton.setOnClickListener(v -> openPhotoCapture());

        ViewCompat.setOnApplyWindowInsetsListener(legacyRoot, (view, insets) -> {
            var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(legacyRoot);
        legacyRoot.setVisibility(View.GONE);
    }
''')

main = main.replace("adapter.notifyDataSetChanged();", "notifyFolderAdapters();")
main = main.replace("addressText.setText(\"Address: \" + address.name());",
                    "addressText.setText(PropertyDisplayName.fromDriveFolderName(address.name()));")
main = main.replace("statusText.setText(message + \": \" + folder.name());",
                    "statusText.setText(message + \": \" + PropertyDisplayName.fromDriveFolderName(folder.name()));")

# Add adapter synchronization before renderCurrentWorkOrder.
needle = "    private void renderCurrentWorkOrder() {"
helper = r'''
    private void notifyFolderAdapters() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (workOrderAdapter != null) {
            workOrderAdapter.notifyDataSetChanged();
        }
    }

'''
main = main.replace(needle, helper + needle, 1)

# Make selected state visible in Work Orders list and hide routine success banner clutter.
main = main.replace(
    "        renderCurrentWorkOrder();\n        statusText.setText(message + \": \" + PropertyDisplayName.fromDriveFolderName(folder.name()));\n        setNotBusy();",
    "        renderCurrentWorkOrder();\n        if (workOrderAdapter != null) { workOrderAdapter.setSelectedId(folder.id()); }\n        statusText.setText(message + \": \" + PropertyDisplayName.fromDriveFolderName(folder.name()));\n        statusText.setVisibility(View.VISIBLE);\n        setNotBusy();")
main = main.replace(
    "        currentWorkOrderText.setText(selectedWorkOrder == null\n                ? \"Selected work order: none\"\n                : \"Selected work order: \" + selectedWorkOrder.name());",
    "        currentWorkOrderText.setText(selectedWorkOrder == null\n                ? \"Select a work order below\"\n                : \"Selected: \" + PropertyDisplayName.fromDriveFolderName(selectedWorkOrder.name()));\n        if (workOrderAdapter != null) {\n            workOrderAdapter.setSelectedId(selectedWorkOrder == null ? null : selectedWorkOrder.id());\n        }")
main = main.replace(
    "            statusText.setText(folders.size() + \" work-order folder\"\n                            + (folders.size() == 1 ? \"\" : \"s\") + \" found.\");",
    "            statusText.setText(folders.size() + \" work order\" + (folders.size() == 1 ? \"\" : \"s\") + \" available\");\n                    statusText.setVisibility(View.GONE);")
main = main.replace(
    "            statusText.setText(message);\n        }\n        setNotBusy();",
    "            statusText.setText(message);\n            statusText.setVisibility(View.VISIBLE);\n        }\n        setNotBusy();")
main = main.replace(
    "            statusText.setText(message);\n        }\n        setNotBusy();\n    }\n\n    private void applySystemBarAppearance",
    "            statusText.setText(message);\n            statusText.setVisibility(View.VISIBLE);\n        }\n        setNotBusy();\n    }\n\n    private void applySystemBarAppearance")
MAIN.write_text(main, encoding="utf-8")

# ---------- Patch PhotoCaptureActivity presentation only ----------
photo = PHOTO.read_text(encoding="utf-8")
photo = photo.replace("import android.os.Bundle;\n", "import android.os.Bundle;\nimport android.graphics.Bitmap;\nimport android.graphics.BitmapFactory;\nimport android.view.LayoutInflater;\nimport android.view.View;\nimport android.widget.ImageView;\n")
photo = photo.replace("import android.widget.ScrollView;\n", "import android.widget.ScrollView;\n\nimport androidx.core.content.ContextCompat;\nimport androidx.core.view.ViewCompat;\nimport androidx.core.view.WindowCompat;\nimport androidx.core.view.WindowInsetsCompat;\nimport androidx.core.view.WindowInsetsControllerCompat;\n")
photo = add_after(photo, "    private Button discardButton;\n", "    private View selectedActionsPanel;\n")

photo = replace_between(photo, "    private void buildUi() {", "    private void beginCameraCapture() {", r'''
    private void buildUi() {
        setContentView(R.layout.screen_photos);
        View root = findViewById(R.id.photos_root);
        statusText = findViewById(R.id.photos_status);
        pendingCountText = findViewById(R.id.photos_pending_count);
        batchSelectionText = findViewById(R.id.photos_batch_selection);
        selectedPhotoText = findViewById(R.id.photos_selected_text);
        preparedPhotoText = findViewById(R.id.photos_prepared_text);
        pendingList = findViewById(R.id.photos_pending_list);
        selectedActionsPanel = findViewById(R.id.photos_selected_panel);
        takePhotoButton = findViewById(R.id.photos_open_camera);
        selectAllReadyButton = findViewById(R.id.photos_select_all);
        clearSelectionButton = findViewById(R.id.photos_clear_selection);
        uploadBatchButton = findViewById(R.id.photos_upload_selected);
        prepareButton = findViewById(R.id.photos_prepare);
        uploadButton = findViewById(R.id.photos_upload_one);
        reconcileButton = findViewById(R.id.photos_reconcile);
        discardButton = findViewById(R.id.photos_discard);

        TextView addressText = findViewById(R.id.photos_address);
        TextView workOrderText = findViewById(R.id.photos_work_order);
        addressText.setText(address == null ? "No property selected"
                : PropertyDisplayName.fromDriveFolderName(address.name()));
        workOrderText.setText(workOrder == null ? "No work order selected"
                : PropertyDisplayName.fromDriveFolderName(workOrder.name()));

        takePhotoButton.setOnClickListener(v -> beginCameraCapture());
        selectAllReadyButton.setOnClickListener(v -> selectAllReadyPhotos());
        clearSelectionButton.setOnClickListener(v -> clearBatchSelection());
        uploadBatchButton.setOnClickListener(v -> uploadSelectedBatch());
        prepareButton.setOnClickListener(v -> prepareSelectedPhoto());
        uploadButton.setOnClickListener(v -> uploadSelectedPhoto());
        reconcileButton.setOnClickListener(v -> reconcileSelectedPhoto());
        discardButton.setOnClickListener(v -> confirmDiscardSelected());
        findViewById(R.id.photos_back).setOnClickListener(v -> finish());
        findViewById(R.id.photos_nav_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.photos_nav_work_orders).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        int barColor = ContextCompat.getColor(this, R.color.home_background);
        getWindow().setStatusBarColor(barColor);
        getWindow().setNavigationBarColor(barColor);

        updateBatchSelectionUi();
        renderSelectedPhoto();
    }
''')

photo = replace_between(photo, "    private void renderPhotoList(", "    private boolean isBatchUploadEligible(", r'''
    private void renderPhotoList(
            List<PendingPhotoRecord> records,
            List<String> unusableQueuedIds) {
        pendingList.removeAllViews();
        pendingCountText.setText("Photos (" + records.size() + ")");

        boolean controlsBusy = PREPARATION_GATE.isBusy() || UPLOAD_GATE.isBusy();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (PendingPhotoRecord record : records) {
            boolean unusable = unusableQueuedIds.contains(record.id());
            boolean prepared = hasPreparedCopy(record.id());
            boolean preparing = PREPARATION_GATE.isPreparing(record.id());
            boolean remoteBusy = isPhotoRemoteBusy(record.id());
            boolean batchEligible = isBatchUploadEligible(record, unusable, prepared);

            View row = inflater.inflate(R.layout.row_photo, pendingList, false);
            CheckBox batchCheckBox = row.findViewById(R.id.photo_row_check);
            ImageView thumbnail = row.findViewById(R.id.photo_row_thumb);
            TextView stateText = row.findViewById(R.id.photo_row_status);
            TextView timeText = row.findViewById(R.id.photo_row_time);

            batchCheckBox.setChecked(batchSelectedPhotoIds.contains(record.id()));
            batchCheckBox.setEnabled(batchEligible && !controlsBusy);
            batchCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    batchSelectedPhotoIds.add(record.id());
                } else {
                    batchSelectedPhotoIds.remove(record.id());
                }
                updateBatchSelectionUi();
            });

            String state = photoStatusLabel(record, unusable, prepared, preparing, remoteBusy);
            stateText.setText(state);
            timeText.setText(formatTime(record.createdAtEpochMs()) + " · …" + shortId(record.id()));
            if (selectedPhotoId != null && selectedPhotoId.equals(record.id())) {
                row.setBackgroundResource(R.drawable.bg_concept_selected);
            }
            loadThumbnail(thumbnail, record);
            row.setOnClickListener(v -> {
                selectedPhotoId = record.id();
                renderSelectedPhoto();
                refreshPhotoList();
            });
            pendingList.addView(row);
        }
        updateBatchSelectionUi();
        renderSelectedPhoto();
    }

    private String photoStatusLabel(
            PendingPhotoRecord record,
            boolean unusable,
            boolean prepared,
            boolean preparing,
            boolean remoteBusy) {
        if (unusable) return "Needs photo data";
        if (remoteBusy && record.state() == PendingPhotoRecord.State.UNCERTAIN) return "Reconciling";
        if (remoteBusy) return "Uploading";
        if (preparing) return "Preparing";
        if (record.state() == PendingPhotoRecord.State.UPLOADED) return "Uploaded";
        if (record.state() == PendingPhotoRecord.State.UNCERTAIN) return "Needs attention";
        if (record.state() == PendingPhotoRecord.State.FAILED) return prepared ? "Ready to retry" : "Retry preparation";
        return prepared ? "Ready to upload" : "Waiting";
    }

    private void loadThumbnail(ImageView view, PendingPhotoRecord record) {
        File source = getPreparedFileOrNull(record.id());
        try {
            if (source == null || !source.isFile() || source.length() <= 0) {
                source = photoStore.imageFile(record);
            }
            if (source == null || !source.isFile() || source.length() <= 0) {
                view.setImageDrawable(null);
                return;
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);
            int sample = 1;
            int target = dp(160);
            while (bounds.outWidth / sample > target * 2 || bounds.outHeight / sample > target * 2) {
                sample *= 2;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, sample);
            Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath(), options);
            view.setImageBitmap(bitmap);
        } catch (Exception ignored) {
            view.setImageDrawable(null);
        }
    }
''')

# Show compact status banner only when there is something to say.
photo = photo.replace("statusText.setText(", "showPhotoStatus(")
# restore field assignment calls accidentally affected in build? no setText in assignments.
# Add helper before showError.
insert_before = "    private void showError("
status_helper = r'''
    private void showPhotoStatus(String message) {
        if (statusText == null) {
            return;
        }
        statusText.setText(message == null ? "" : message);
        statusText.setVisibility(message == null || message.trim().isEmpty() ? View.GONE : View.VISIBLE);
    }

'''
photo = photo.replace(insert_before, status_helper + insert_before, 1)
# showError itself still needs direct TextView.setText, fix likely replacement within method below.
photo = photo.replace("showPhotoStatus(prefix + (detail == null ? \".\" : \": \" + detail));",
                      "showPhotoStatus(prefix + (detail == null ? \".\" : \": \" + detail));")
# selected panel visibility.
photo = photo.replace(
    "        if (selectedPhotoId == null) {\n            selectedPhotoText.setText(\"Selected temporary photo: none\");",
    "        if (selectedPhotoId == null) {\n            if (selectedActionsPanel != null) selectedActionsPanel.setVisibility(View.GONE);\n            selectedPhotoText.setText(\"Selected temporary photo: none\");")
photo = photo.replace(
    "        String attempt = selected.uploadAttemptCount() > 0",
    "        if (selectedActionsPanel != null) selectedActionsPanel.setVisibility(View.VISIBLE);\n\n        String attempt = selected.uploadAttemptCount() > 0")
PHOTO.write_text(photo, encoding="utf-8")

# ---------- Focused structural test ----------
write(ROOT / "app/src/androidTest/java/com/inandout/fieldphotoprep/Concept3UiStructureInstrumentedTest.java", r'''
package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class Concept3UiStructureInstrumentedTest {
    @Test
    public void concept3ScreensExposeRealFieldActions() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View home = inflater.inflate(R.layout.screen_home_properties, null, false);
        assertNotNull(home.findViewById(R.id.home_property_list));
        assertNotNull(home.findViewById(R.id.home_new_address_button));
        assertNotNull(home.findViewById(R.id.home_nav_work_orders));

        View work = inflater.inflate(R.layout.screen_work_orders, null, false);
        assertNotNull(work.findViewById(R.id.work_order_list));
        assertNotNull(work.findViewById(R.id.work_order_create_button));
        assertNotNull(work.findViewById(R.id.work_order_photos));
        assertNotNull(work.findViewById(R.id.work_order_maintenance));

        View photos = inflater.inflate(R.layout.screen_photos, null, false);
        assertNotNull(photos.findViewById(R.id.photos_open_camera));
        assertNotNull(photos.findViewById(R.id.photos_pending_list));
        View upload = photos.findViewById(R.id.photos_upload_selected);
        assertNotNull(upload);
        assertTrue(upload.getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
''')

# ---------- Implementation record ----------
write(ROOT / "docs/CONCEPT_3_NON_CAMERA_UI_IMPLEMENTATION_RECORD_2026-09-12.md", r'''
# Concept 3 Non-Camera UI Implementation Record

Date: 2026-09-12

Status: IMPLEMENTED ON FEATURE BRANCH — AUTOMATED VERIFICATION REQUIRED

Branch: `feat/concept-3-ui-makeover-20260912`

Rollback baseline: `e77b83bf07505cc586fb8766cb8623db51e35b7`

## Scope

Replace the rejected non-camera development-style presentation with the operator-selected Concept 3 Hybrid Field App direction while preserving existing Field Photo Prep behavior.

Changed presentation surfaces:

- Home / Properties
- Work Orders
- Photos / upload queue

Protected and intentionally untouched:

- `CameraCaptureActivity`
- CameraX capture behavior
- protected-original semantics
- Drive provider identities and master-tree permission model
- address/work-order duplicate, create, reuse, and Clear & Reuse behavior
- queue state machine
- upload/retry/UNCERTAIN/reconciliation behavior
- confirmed-success cleanup semantics

## Concept 3 mapping

The reference image is visual authority, but fake route/map/schedule features are not copied.

- Home uses the Concept 3 compact app bar, strong green status/context card, dense property cards, blue primary action, and persistent bottom navigation.
- Work Orders uses a compact property context card, real selectable work-order cards, blue Photos action, compact new-dated-work-order controls, and collapsed maintenance actions.
- Photos uses a compact work-order context card, prominent blue Open Camera action, real local thumbnails when bytes exist, explicit checkbox selection, compact status rows, and sticky green Upload Selected action.
- Bottom navigation contains only real Field Photo Prep destinations: Home, Work Orders, Photos.

## Read/write surfaces

This makeover introduces no new persisted schema and no new Drive write implementation. UI actions continue to call the existing MainActivity/DriveClient and PhotoCaptureActivity queue/upload owners.

Thumbnail decoding reads only existing local protected/prepared files for display and does not modify them.

## Verification

Focused coverage: `Concept3UiStructureInstrumentedTest` plus the existing property display tests.

Final requirement before staging: one complete Android CI pass on the exact executable runtime head. Physical Samsung acceptance remains the final visual gate.
''')

# Camera lock: fail the transformation if the protected camera file was touched by checkout/patch.
status = subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True)
if "CameraCaptureActivity.java" in status:
    raise SystemExit("Protected CameraCaptureActivity changed unexpectedly")

print("Concept 3 UI transformation applied.")
