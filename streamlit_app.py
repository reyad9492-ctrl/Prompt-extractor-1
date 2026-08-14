import streamlit as st
import requests
import base64
import json
from PIL import Image
import io

# =====================================================================
# INSTALLATION & RUN INSTRUCTIONS
# =====================================================================
# To run this Streamlit application locally, follow these steps:
# 
# 1. Install Python 3.8+ on your system.
# 2. Open a terminal / command prompt.
# 3. Install the required libraries by running:
#    pip install streamlit requests pillow
# 4. Save this code as `streamlit_app.py`
# 5. Run the Streamlit application using the command:
#    streamlit run streamlit_app.py
# 6. Open the URL shown in the terminal (usually http://localhost:8501) in your browser.
# =====================================================================

# Set page configuration with a custom dark/stylish visual profile
st.set_page_config(
    page_title="Prompt Blender",
    page_icon="🔮",
    layout="wide",
    initial_sidebar_state="expanded"
)

# Custom Atmospheric CSS to style the Streamlit interface
st.markdown("""
<style>
    /* Dark Slate Canvas Theme - Immersive UI SPEC */
    .stApp {
        background-color: #1C1B1F !important;
        color: #E6E1E5 !important;
    }
    
    /* Category tag layout style */
    .category-tag {
        font-family: 'Helvetica Neue', Arial, sans-serif;
        font-size: 11px;
        font-weight: 700;
        letter-spacing: 1.5px;
        color: #D0BCFF;
        text-transform: uppercase;
        margin-bottom: 2px;
    }

    /* Elegant Custom Title styling */
    .main-title {
        font-family: 'Helvetica Neue', Arial, sans-serif;
        font-size: 2.25rem;
        font-weight: 800;
        color: #E6E1E5;
        margin-top: 0px;
        margin-bottom: 0.5rem;
    }
    .subtitle {
        font-size: 1rem;
        color: #CAC4D0;
        margin-bottom: 2rem;
    }
    
    /* Custom Card Container rounded-3xl with border-[#49454F] */
    .custom-card {
        background-color: #2B2930;
        padding: 24px;
        border-radius: 24px;
        border: 1px solid #49454F;
        margin-bottom: 20px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    }
    
    /* Sidebar styling overrides */
    .sidebar-header {
        font-size: 1.2rem;
        font-weight: 700;
        color: #D0BCFF;
        margin-bottom: 12px;
        letter-spacing: 0.5px;
    }
    
    /* Result code block styling */
    .prompt-box {
        background-color: #1C1B1F;
        padding: 20px;
        border-radius: 16px;
        border: 1px solid #49454F;
        font-family: 'Courier New', Courier, monospace;
        font-size: 1rem;
        color: #E6E1E5;
        white-space: pre-wrap;
    }

    /* Primary Accent styled Generate Button inside col2 */
    div.stButton > button {
        background-color: #D0BCFF !important;
        color: #381E72 !important;
        border-radius: 9999px !important;
        font-weight: bold !important;
        border: none !important;
        padding: 12px 24px !important;
        width: 100%;
        transition: all 0.2s ease;
    }
    div.stButton > button:hover {
        background-color: #dfceff !important;
    }
    div.stButton > button:active {
        transform: scale(0.98);
    }
</style>
""", unsafe_allow_html=True)

# Main Banner / Title with Immersive UI tag hierarchy
st.markdown("<p class='category-tag'>AI Vision</p>", unsafe_allow_html=True)
st.markdown("<h1 class='main-title'>Prompt Blender</h1>", unsafe_allow_html=True)
st.markdown("<p class='subtitle'>Blend any Instagram video's cinematic identity with your personal features to engineer high-fidelity visual prompts.</p>", unsafe_allow_html=True)

# Immersive Interactive Guide Expander
with st.expander("📖 Interactive Quick Guide: Learn how to blend in 4 easy steps", expanded=False):
    st.markdown("""
    <div style='background-color: #2B2930; padding: 20px; border-radius: 16px; border: 1px solid #49454F;'>
        <h4 style='color: #D0BCFF; margin-top: 0;'>🎨 Multiplatform Visual Synergy</h4>
        <p style='font-size: 14px; color: #CAC4D0;'>Follow this step-by-step guide to fuse your personal visual identity into any high-end cinematic post atmosphere:</p>
        <div style='display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 12px;'>
            <div style='background-color: #1C1B1F; padding: 12px; border-radius: 12px; border: 1px solid #49454F;'>
                <strong style='color: #D0BCFF;'>Step 1: Upload Portrait Headshot</strong>
                <p style='font-size: 12px; color: #CAC4D0; margin: 4px 0 0 0;'>Provide a clean front-facing face photo. Our multimodal synthesis maps your unique hair flow, skin tones, and facial contours.</p>
            </div>
            <div style='background-color: #1C1B1F; padding: 12px; border-radius: 12px; border: 1px solid #49454F;'>
                <strong style='color: #D0BCFF;'>Step 2: Define Scene Style Source</strong>
                <p style='font-size: 12px; color: #CAC4D0; margin: 4px 0 0 0;'>Paste an Instagram post / Reel video link, or drag-and-drop a screenshot of the movie scene or lighting you want to copy.</p>
            </div>
            <div style='background-color: #1C1B1F; padding: 12px; border-radius: 12px; border: 1px solid #49454F;'>
                <strong style='color: #D0BCFF;'>Step 3: Tune Matrix Settings</strong>
                <p style='font-size: 12px; color: #CAC4D0; margin: 4px 0 0 0;'>Select ChatGPT (optimized for DALL-E sentence structures) or Gemini (for precise photographic characteristics) and select your target format.</p>
            </div>
            <div style='background-color: #1C1B1F; padding: 12px; border-radius: 12px; border: 1px solid #49454F;'>
                <strong style='color: #D0BCFF;'>Step 4: Spark Magic & Generate</strong>
                <p style='font-size: 12px; color: #CAC4D0; margin: 4px 0 0 0;'>Tap <strong>GENERATE BLENDED PROMPT</strong>. Copy the outputs and paste them into ChatGPT or Gemini to see yourself fully integrated into the scene!</p>
            </div>
        </div>
    </div>
    """, unsafe_allow_html=True)
    st.write("")

# Sidebar Configuration
st.sidebar.markdown("<div class='sidebar-header'>🔮 API Settings</div>", unsafe_allow_html=True)

# API Secret Management Setup
api_key = st.sidebar.text_input(
    "Gemini API Key", 
    type="password", 
    placeholder="Enter your Gemini Key her...",
    help="You can get an API key from Google AI Studio. Note: Your key is kept local and never stored externally."
)

fallback_api_key = st.sidebar.text_input(
    "Optional: Custom Prompt Prefix",
    placeholder="e.g. Highly realistic photograph...",
    help="Add a custom starter text for all generated prompts."
)

st.sidebar.markdown("---")
st.sidebar.markdown("<div class='sidebar-header'>🎨 Output Settings</div>", unsafe_allow_html=True)

target_engine = st.sidebar.radio(
    "Target Image Generator",
    ["ChatGPT", "Gemini"],
    index=0
)

aspect_ratio = st.sidebar.selectbox(
    "Aspect Ratio",
    ["16:9 (Cinematic/Landscape)", "9:16 (Story/Portrait)", "1:1 (Square)", "4:5 (Instagram Grid)"],
    index=0
)

prompt_style = st.sidebar.selectbox(
    "Creative Direction Modifiers",
    [
        "Cinematic Portrait (Atmospheric, raw)",
        "Cyberpunk Neon (Vivid, high-contrast glow)",
        "Dreamy Pastel (Soft lighting, retro vibe)",
        "Retro Vintage (Film grain, nostalgic 90s aesthetic)",
        "Editorial High-Fashion (Studio lighting, bold poses)"
    ],
    index=0
)

# Render main page split into two panels: Input Form and Output Result
col1, col2 = st.columns([1, 1.1])

with col1:
    st.markdown("<div class='custom-card'>", unsafe_allow_html=True)
    st.subheader("1. Enter Style Reference")
    
    instagram_link = st.text_input(
        "Instagram Video or Post Link",
        placeholder="https://www.instagram.com/p/C6Z...",
        help="Paste the link to the visual style or video theme you want to copy."
    )
    
    style_image_file = st.file_uploader(
        "Upload Video Screenshot / Style Reference (Highly Recommended)",
        type=["png", "jpg", "jpeg", "webp"],
        help="Since video links are sometimes restricted by Instagram, uploading a screenshot of the visual style ensures Gemini can analyze the lighting, background, apparel, and color grading perfectly."
    )
    
    if style_image_file:
        st.image(style_image_file, caption="Selected Style Reference Visual", width=250)
        
    st.markdown("</div>", unsafe_allow_html=True)
    
    st.markdown("<div class='custom-card'>", unsafe_allow_html=True)
    st.subheader("2. Upload Your Portrait Photo")
    
    user_image_file = st.file_uploader(
        "Choose Your Face Photo",
        type=["png", "jpg", "jpeg", "webp"],
        help="Upload a clear front-facing portrait or selfie. Our AI parses facial features, hair type, and structural properties to preserve your identity in the output scene."
    )
    
    if user_image_file:
        st.image(user_image_file, caption="Your Avatar / Portrait Reference", width=250)
        
    st.markdown("</div>", unsafe_allow_html=True)

# Helper function to convert raw stream to base64
def file_to_base64_data(uploaded_file):
    if uploaded_file is not None:
        bytes_data = uploaded_file.getvalue()
        return base64.b64encode(bytes_data).decode("utf-8")
    return None

# Mapping aspect ratios to Midjourney commands
ar_suffix = {
    "16:9 (Cinematic/Landscape)": " --ar 16:9",
    "9:16 (Story/Portrait)": " --ar 9:16",
    "1:1 (Square)": " --ar 1:1",
    "4:5 (Instagram Grid)": " --ar 4:5"
}

with col2:
    st.subheader("🚀 Generator Engine")
    
    if st.button("Spark Magic: Generate Detailed Prompt", type="primary", use_container_width=True):
        if not api_key:
            st.error("🔑 Please enters a Gemini API key in the sidebar to run the generator!")
        elif not user_image_file:
            st.warning("👤 Please upload your portrait photo under Step 2 to preserve your identity.")
        else:
            with st.spinner("Analyzing style reference and facial features to engineer your custom prompt..."):
                try:
                    # Collect Base64 data
                    user_b64 = file_to_base64_data(user_image_file)
                    style_b64 = file_to_base64_data(style_image_file) if style_image_file else None
                    
                    # Prepare System Prompt Request structure
                    prompt_text = f"""
You are an expert AI prompt engineer specializing in creating detailed visual prompts for {target_engine}.

Your task is to craft an incredibly detailed image generation prompt that blends a "User's Identity" (captured from their uploaded face photo) with a "Visual Style" (from an Instagram video/theme, described by a visual style screenshot and/or a link).

Inputs provided:
1. Instagram Link/Context (if provided): {instagram_link if instagram_link else "Not provided"}
2. Target Style Engine: {target_engine}
3. Modifier Creative Vibe: {prompt_style}
{f"4. User Custom Prefix: {fallback_api_key}" if fallback_api_key else ""}

Instructions:
1. Analyze the User's uploaded face photo. Focus on capturing their immutable identity and features: facial structure, key facial features, hair type/texture/color, eye shape, and gender expression, so that an image generator can accurately reproduce their likeness. Keep the description respectful and highly detailed.
2. Analyze the visual style reference photo (if provided) and/or use stylistic assumptions from the theme '{prompt_style}' (such as lighting, camera angles, color grading, apparel, backdrop, overall atmosphere).
3. Synthesize both. Write a cohesive, single-paragraph prompt.
4. Structure the output prompt exactly as required by the chosen style engine ({target_engine}):
   - For ChatGPT (DALL-E 3): Focus on a natural English visual description. Start with "An artistic photograph of..." or "A detailed 3D render of...". Keep descriptions vivid, poetic, and cinematic, incorporating color, scenery, lighting, and costume detail descriptive sentences. Avoid technical parameters.
   - For Gemini (Imagen 3): Frame the prompt with photographic specificity. Include depth of field, rich style descriptors (e.g. vintage print, cinematic scan), exact shadow gradients, and high-fidelity sensory texture words. Avoid syntax codes.

Output your response strictly in JSON format as shown below. Always enclose it in standard JSON syntax so that it can be cleanly parsed.

```json
{{
  "analyzedFace": "Detailed textual breakdown of the user's face, hair, and identity features to preserve.",
  "analyzedStyle": "Detailed textual breakdown of the visual theme, lighting, outfit, and background.",
  "generatedPrompt": "The final complete, ready-to-copy image generation prompt with {aspect_ratio} specifications built-in."
}}
```
"""

                    # Call direct Gemini API REST endpoint
                    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key={api_key}"
                    
                    # Assemble parts
                    parts = [{"text": prompt_text}]
                    
                    # Add User Face Image
                    parts.append({
                        "inlineData": {
                            "mimeType": "image/jpeg",
                            "data": user_b64
                        }
                    })
                    
                    # Add Visual Style Image if uploaded
                    if style_b64:
                        parts.append({
                            "inlineData": {
                                "mimeType": "image/jpeg",
                                "data": style_b64
                            }
                        })
                        
                    request_payload = {
                        "contents": [{
                            "parts": parts
                        }],
                        "generationConfig": {
                            "responseMimeType": "application/json",
                            "temperature": 0.55
                        }
                    }
                    
                    # Post Request to API
                    headers = {"Content-Type": "application/json"}
                    response = requests.post(url, data=json.dumps(request_payload), headers=headers, timeout=60)
                    
                    if response.status_code != 200:
                        # Attempt fallback model (gemini-3.5-flash) if 2.5-flash-image is not available or disabled
                        url_fallback = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key={api_key}"
                        response = requests.post(url_fallback, data=json.dumps(request_payload), headers=headers, timeout=60)
                    
                    if response.status_code == 200:
                        response_json = response.json()
                        raw_text = response_json["candidates"][0]["content"]["parts"][0]["text"]
                        
                        # Parse JSON from string
                        try:
                            clean_text = raw_text.strip()
                            if clean_text.startswith("```json"):
                                clean_text = clean_text[7:]
                            if clean_text.endswith("```"):
                                clean_text = clean_text[:-3]
                            clean_text = clean_text.strip()
                            
                            data = json.loads(clean_text)
                            
                            face_profile = data.get("analyzedFace", "Identity captured successfully.")
                            style_profile = data.get("analyzedStyle", "Visual style modeled successfully.")
                            final_prompt = data.get("generatedPrompt", "")
                            
                            # Standardize aspect ratio settings for target engines
                            ratio_clean = aspect_ratio.split(' ')[0]
                            if target_engine == "ChatGPT":
                                if "aspect ratio" not in final_prompt.lower():
                                    final_prompt += f", aspect ratio {ratio_clean}"
                            else: # Gemini
                                if "aspect ratio" not in final_prompt.lower():
                                    final_prompt += f", in {ratio_clean} aspect ratio"
                            
                            st.success("✨ Prompt Engineered Successfully!")
                            st.markdown("### 🗂️ Analysis Report")
                            
                            ta1, ta2 = st.tabs(["👤 Face Profile (Identity Map)", "🎬 Style Reference Profile"])
                            with ta1:
                                st.info(face_profile)
                            with ta2:
                                st.info(style_profile)
                                
                            st.markdown("### 🔮 Your Generated Prompt")
                            st.markdown(f"<div class='prompt-box'>{final_prompt}</div>", unsafe_allow_html=True)
                            
                            # Copy action
                            st.text_area("Plain-text copy helper (Select & Copy):", value=final_prompt, height=100)
                            
                        except Exception as parse_err:
                            st.warning("Prompt generated but JSON structure was unparseable. Showing raw text output:")
                            st.code(raw_text)
                    else:
                        st.error(f"API Request Failed (HTTP {response.status_code}): {response.text}")
                        
                except Exception as api_err:
                    st.error(f"Failed to generate prompt: {str(api_err)}")
                    
    else:
        # Prompt placeholder visual State
        st.info("💡 Complete inputs on the left and click 'Spark Magic' to trigger prompt synthesis.")
        st.markdown("""
        ### How it works:
        1. **Identity Capture**: Facial dimensions, expression, shape, hair, and gaze are cataloged from your photo.
        2. **Visual Style Synthesis**: Lighting dynamics, clothing style, composition, backdrop vibe, and scene density are decoded from the Instagram link and screenshot.
        3. **Prompt Compilation**: The engine coordinates the descriptors to build a cohesive ChatGPT or Gemini prompt where you are fully integrated inside the cinematic setting.
        """)
