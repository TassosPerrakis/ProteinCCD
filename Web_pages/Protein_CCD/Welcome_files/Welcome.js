// Created by iWeb 2.0.4 local-build-20090908

setTransparentGifURL('Media/transparent.gif');function applyEffects()
{var registry=IWCreateEffectRegistry();registry.registerEffects({reflection_1:new IWReflection({opacity:0.50,offset:1.00}),shadow_0:new IWShadow({blurRadius:10,offset:new IWPoint(4.2426,4.2426),color:'#000000',opacity:0.750000}),reflection_0:new IWReflection({opacity:0.50,offset:-21.22}),shadow_1:new IWShadow({blurRadius:10,offset:new IWPoint(4.2426,4.2426),color:'#000000',opacity:0.750000})});registry.applyEffects();}
function hostedOnDM()
{return false;}
function onPageLoad()
{loadMozillaCSS('Welcome_files/WelcomeMoz.css')
adjustLineHeightIfTooBig('id1');adjustFontSizeIfTooBig('id1');adjustLineHeightIfTooBig('id2');adjustFontSizeIfTooBig('id2');adjustLineHeightIfTooBig('id3');adjustFontSizeIfTooBig('id3');adjustLineHeightIfTooBig('id4');adjustFontSizeIfTooBig('id4');Widget.onload();fixAllIEPNGs('Media/transparent.gif');IMpreload('Welcome_files','shapeimage_4','0');IMpreload('Welcome_files','shapeimage_5','0');IMpreload('Welcome_files','shapeimage_6','0');IMpreload('Welcome_files','shapeimage_7','0');IMpreload('Welcome_files','shapeimage_7','1');applyEffects()}
function onPageUnload()
{Widget.onunload();}
