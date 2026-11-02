"""Rebuild verified 0.4.1 client artifacts from immutable public 0.4.0 inputs.

2026-11-03 Asia/Tokyo (UTC+09:00). No credentials or player data.
"""
from pathlib import Path
import datetime as dt
import hashlib
import io
import json
import shutil
import urllib.request
import zipfile
ROOT=Path(__file__).resolve().parent
OUT=ROOT/'out';OUT.mkdir(exist_ok=False)
cache=ROOT/'cache';cache.mkdir(exist_ok=True)
baseline=json.loads((ROOT/'baseline.json').read_text(encoding='utf-8'))
menu=ROOT/'richlite-menu-1.0.0.jar'
raw=menu.read_bytes()
mod={'path':'mods/'+menu.name,'sha512':hashlib.sha512(raw).hexdigest(),'sha256':hashlib.sha256(raw).hexdigest(),'bytes':len(raw)}
old=baseline['packages']
stamp=baseline['build_at']
results=[]
for row in old:
    source=cache/row['filename'];row['path']=str(source)
    if not source.exists():
        request=urllib.request.Request('https://github.com/324641aliyun/richlite-client/releases/download/v0.4.0/'+row['filename'],headers={'User-Agent':'RichLite-native-menu-release'})
        with urllib.request.urlopen(request,timeout=180) as response,source.open('xb') as target:shutil.copyfileobj(response,target)
    assert hashlib.sha256(source.read_bytes()).hexdigest()==row['sha256']
    lite=row['tier']=='lite';filename=row['filename'].replace('0.4.0','0.4.1');target=OUT/filename
    prefix='overrides' if lite else 'game'
    with zipfile.ZipFile(source) as archive:
        content={i.filename:archive.read(i) for i in archive.infolist() if not i.is_dir()}
        modes={i.filename:i.external_attr for i in archive.infolist()}
    existing_mods=json.loads(content['MODS-EXACT.json'])
    assert all(x['path']!=mod['path'] for x in existing_mods)
    content[prefix+'/'+mod['path']]=raw
    content['MODS-EXACT.json']=(json.dumps([*existing_mods,mod],ensure_ascii=False,indent=2)+'\n').encode()
    for name in ['README-安装说明.md','pack-settings.json','modrinth.index.json']:
        if name in content:content[name]=content[name].decode('utf-8').replace('0.4.0','0.4.1').encode('utf-8')
    guide=f'''# 全局菜单更新 · RichLite 0.4.1

生成时间：{stamp}（Asia/Tokyo，UTC+09:00）。

进入本服后按 Backspace 打开 Minecraft 原生六行箱子菜单。
也可输入 /menu 或 /servermenu；Android 可用聊天命令或在启动器映射 Backspace。
控制设置 → 按键 → 我的世界.cc 中可以改键。聊天、搜索与其它输入界面不拦截退格。

包括个人资料、主城与传送、领地与队伍、任务与玩法、在线玩家、金融入口、官网与帮助、服务器状态。
传送和领地继续遵守原权限、冷却和保护；金融未正式开放时明确标注不可用。
管理入口按玩家权限显示；官网后台另按 LittleSkin 账号验证。

升级请新建独立实例或完整解压到新目录，不覆盖自己的存档、配置或会话。
现有 0.4.0 客户端也可只将 richlite-menu-1.0.0.jar 加入本实例 mods 目录；重启游戏后生效。
原有玩法模组、光影与 ReGlass 文件保持原字节；启动程序仅更新显示版本号。
本包不包含账号、密码、令牌、存档、日志或测试模组。
'''
    content['全局菜单与升级说明.md']=guide.encode('utf-8')
    content['third-party/richlite-menu-source.txt']=b'MyWorld.cc Native Global Menu 1.0.0\nMIT License, Copyright (c) 2026 MyWorld.cc\nNative Minecraft container screen; no custom GUI renderer.\n'
    if not lite:
        package=json.loads(content['launcher/package.json']);package['version']='0.4.1';package['mods'].append(mod)
        content['launcher/package.json']=(json.dumps(package,ensure_ascii=False,indent=2)+'\n').encode()
        launcher=io.BytesIO()
        with zipfile.ZipFile(io.BytesIO(content['launcher/RichLiteLauncher.jar'])) as before,zipfile.ZipFile(launcher,'w',zipfile.ZIP_DEFLATED) as after:
            replacements=0
            for entry in before.infolist():
                value=before.read(entry)
                if entry.filename=='cc/myworld/richlite/Launcher.class':
                    marker=b'\x01\x00\x050.4.0';assert value.count(marker)==1
                    value=value.replace(marker,b'\x01\x00\x050.4.1');replacements+=1
                after.writestr(entry,value)
            assert replacements==1
        content['launcher/RichLiteLauncher.jar']=launcher.getvalue()
        content['third-party/RichLiteLauncher.java']=content['third-party/RichLiteLauncher.java'].replace(b'VERSION="0.4.0"',b'VERSION="0.4.1"')
    else:
        index=json.loads(content['modrinth.index.json']);assert index['versionId']=='0.4.1-lite'
    receipt=json.loads(content['BUILD-RECEIPT.json']);receipt.update({'created_at':stamp,'version':'0.4.1','jars':receipt['jars']+1,'bundled_mods':receipt['bundled_mods']+1,'menu_mod':mod,'base_0_4_0_sha256':row['sha256'],'existing_files_preserved_except_version_and_manifests':True})
    content['BUILD-RECEIPT.json']=(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n').encode()
    files=[{'path':name,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()} for name,data in sorted(content.items()) if name!='FILES-SHA256.json']
    content['FILES-SHA256.json']=(json.dumps(files,ensure_ascii=False,indent=2)+'\n').encode()
    with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED,compresslevel=7) as archive:
        for name,data in content.items():
            info=zipfile.ZipInfo(name);info.create_system=3;info.external_attr=modes.get(name,0o100644<<16);info.compress_type=zipfile.ZIP_DEFLATED
            archive.writestr(info,data,compresslevel=7)
    with zipfile.ZipFile(target) as archive,zipfile.ZipFile(source) as before:
        assert archive.testzip() is None
        for item in files:assert hashlib.sha256(archive.read(item['path'])).hexdigest()==item['sha256']
        for name in before.namelist():
            if name!='launcher/RichLiteLauncher.jar' and (name.endswith('.jar') or name.startswith(prefix+'/shaderpacks/') or name==prefix+'/config/iris.properties'):assert archive.read(name)==before.read(name),name
        assert not any('qa-only' in n or '/saves/' in n or '/logs/' in n for n in archive.namelist())
    digest=hashlib.sha256(target.read_bytes()).hexdigest();target.with_suffix('.zip.sha256').write_text(digest+'  '+filename+'\n',encoding='ascii')
    results.append({'created_at':stamp,'version':'0.4.1-lite' if lite else '0.4.1','tier':row['tier'],'filename':filename,'path':str(target),'bytes':target.stat().st_size,'sha256':digest,'old_jars_byte_identical':len(existing_mods),'new_menu_mod':mod,'all_entry_hashes_verified':True})
(OUT/'build-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(results,ensure_ascii=True,indent=2))
