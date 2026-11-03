#!/bin/bash
set -e

SRC="/tmp/jmtube_master.png"

# Generate crisp 512x512 icon
convert -size 512x512 xc:'#0F0F0F' \
  -fill '#FF0000' -draw "roundrectangle 60,120 452,392 70,70" \
  -fill '#FFFFFF' -draw "polygon 220,196 340,256 220,316" \
  -fill '#FFFFFF' -pointsize 64 -font DejaVu-Sans-Bold -draw "text 100,278 'JM'" \
  PNG32:$SRC

BASE="app/src/main/res"
for spec in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  dir="$BASE/mipmap-${spec%:*}"
  size="${spec#*:}"
  mkdir -p "$dir"
  convert "$SRC" -resize "${size}x${size}!" "PNG32:$dir/ic_launcher.png"
  rm -f "$dir/ic_launcher.webp"
  radius=$((size / 2))
  convert "$SRC" -resize "${size}x${size}!" \
    \( -size "${size}x${size}" xc:none -fill white \
       -draw "circle $radius,$radius $radius,0" \) \
    -alpha set -compose DstIn -composite "PNG32:$dir/ic_launcher_round.png"
  rm -f "$dir/ic_launcher_round.webp"
done

echo "Icons generated successfully."
