#!/usr/bin/env python3

import os
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.misc.transform import Transform

def text_to_svg_path(font_path, text, font_size=48, letter_spacing=0.02):
    """Convert text to SVG paths using the specified font."""
    
    # Load the font
    font = TTFont(font_path)
    
    # Get the cmap table for character to glyph mapping
    cmap = font.getBestCmap()
    
    # Get units per em for scaling
    units_per_em = font['head'].unitsPerEm
    scale = font_size / units_per_em
    
    # Calculate letter spacing in font units
    letter_spacing_units = letter_spacing * font_size / scale
    
    # SVG path data
    svg_paths = []
    current_x = 0
    
    # Get glyph set
    glyph_set = font.getGlyphSet()
    
    for char in text:
        # Get glyph name for character
        glyph_name = cmap.get(ord(char))
        if not glyph_name:
            continue
            
        # Get the glyph
        glyph = glyph_set[glyph_name]
        
        # Create transform for positioning and scaling
        transform = Transform()
        transform = transform.translate(current_x, font_size * 1.5)  # Position at baseline
        transform = transform.scale(scale, -scale)  # Flip Y axis for SVG
        
        # Create SVG path pen
        svg_pen = SVGPathPen(glyph_set)
        transform_pen = TransformPen(svg_pen, transform)
        
        # Draw the glyph
        glyph.draw(transform_pen)
        
        # Get the path
        path = svg_pen.getCommands()
        if path:
            svg_paths.append(path)
        
        # Move to next character position
        current_x += glyph.width * scale + letter_spacing_units * scale
    
    # Combine all paths
    combined_path = ' '.join(svg_paths)
    
    # Calculate total width
    total_width = current_x
    
    return combined_path, total_width

def create_thorn_logo_svg():
    """Create the Thorn logo SVG with gradient fill."""
    
    # Get the font path
    font_path = os.path.join(os.path.dirname(__file__), '..', 'Angelos.ttf')
    
    # Convert text to path
    path_data, text_width = text_to_svg_path(font_path, "Thorn", font_size=48, letter_spacing=0.02)
    
    # Add some padding
    svg_width = text_width + 40
    svg_height = 80
    
    # Create the SVG with gradient and skew transform
    svg_content = f'''<svg width="{svg_width}" height="{svg_height}" viewBox="0 0 {svg_width} {svg_height}" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" style="stop-color:#9333ea;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#2563eb;stop-opacity:1" />
    </linearGradient>
  </defs>
  <g transform="translate(20, 0) skewX(-2)">
    <path d="{path_data}" fill="url(#gradient)" />
  </g>
</svg>'''
    
    # Write the SVG file
    output_path = os.path.join(os.path.dirname(__file__), '..', 'assets', 'thorn-logo-text.svg')
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    with open(output_path, 'w') as f:
        f.write(svg_content)
    
    print(f"SVG logo created at: {output_path}")
    print(f"Dimensions: {svg_width}x{svg_height}")

if __name__ == "__main__":
    try:
        create_thorn_logo_svg()
    except ImportError:
        print("Error: fonttools not installed")
        print("Please install it with: pip install fonttools")
    except Exception as e:
        print(f"Error: {e}")