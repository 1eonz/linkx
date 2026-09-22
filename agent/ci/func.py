import os
import shutil
import toml
import yaml
import hashlib
import re
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

from common import utils, log

script_root = utils.get_workspace_root()
workspace_root = script_root.parent
date_part = datetime.now().strftime("%Y%m%d")
now = datetime.now().strftime("%Y%m%d%H%M%S")
LOCAL_TIME = now


@dataclass
class Module:
    path: str
    name: str
    image_name: str
    version: str
    arch: str


@dataclass
class PackageConf:
    modules: list[Module]
    third_imgs_prefix: list[str]
    version: str
    arch: str
    codeRepo: str
    CommitID: str
    codeBranch: str
    gitee_repo: str
    gitee_api_token: str
    gitee_backup: str


def build_web():
    utils.do_subprocess(f"""
        cd {workspace_root / "web"};
        #nvm use 22.11.0
        export NODE_HOME="/opt/buildtools/node-v22.2.0"
        export PATH="/opt/buildtools/node-v22.2.0/bin:$PATH"
        npm install -g pnpm@10.33.4
        pnpm install
        pnpm build
    """, f"build web")


def read_conf() -> PackageConf:
    p = script_root / "package.toml"
    ci_env = script_root / "ci_env.toml"
    gitee_env = script_root / "gitee_env.toml"
    confs = toml.load(p)
    if ci_env.exists():
        v_conf = toml.load(ci_env)
    else:
        v_conf = {}
    if ci_env.exists():
        gitee_conf = toml.load(gitee_env)
    else:
        gitee_conf = {}
    log.info(f"read conf:{p} {v_conf} {gitee_conf}")
    version = v_conf.get("commons", {"version": now}).get("version", now)
    arch = v_conf.get("commons", {"arch": "amd64"}).get("arch", "amd64")
    codeRepo = v_conf.get("commons", {"codeRepo": now}).get("codeRepo", now)
    CommitID = v_conf.get("commons", {"CommitID": now}).get("CommitID", now)
    codeBranch = v_conf.get("commons", {"codeBranch": now}).get("codeBranch", now)
    gitee_repo = gitee_conf.get("commons", {"gitee_repo": now}).get("gitee_repo", now)
    gitee_api_token = gitee_conf.get("commons", {"gitee_api_token": now}).get("gitee_api_token", now)
    gitee_backup = gitee_conf.get("commons", {"gitee_backup": now}).get("gitee_backup", now)
    return PackageConf(
        [Module(**conf, version=version, arch=arch) for conf in confs["modules"]],
        [v for k, v in confs.get("third_imgs_prefix", {}).items()],
        version,
        arch,
        codeRepo,
        CommitID,
        codeBranch,
        gitee_repo,
        gitee_api_token,
        gitee_backup
    )

def read_patch_id() -> str | None:
    candidates = [
        script_root / "app-info.yaml",
        workspace_root.parent / "meta" / "app-info-agent.yaml",
        workspace_root / "meta" / "app-info-agent.yaml",
    ]
    found_file = None
    for path in candidates:
        if path.exists():
            found_file = path
            break
    if found_file is None:
        log.error(f"app-info yaml not found in candidates: {candidates}")
        return None
    with open(found_file, 'r', encoding='utf-8') as f:
        data = yaml.safe_load(f)
    app_info_list = data.get("app-info", []) if data else []
    if not app_info_list:
        log.error(f"app-info section empty in {found_file}")
        return None
    patch_id = app_info_list[0].get("version_patch_id")
    if not patch_id:
        log.error(f"version_patch_id not found in {found_file}")
        return None
    log.info(f"read version_patch_id: {patch_id} from {found_file}")
    return str(patch_id)

def maven_package():
    os.chdir(workspace_root)
    shutil.copy(workspace_root / "ci/maven/mvn_build.sh", workspace_root / "server/mvn_build.sh")
    shutil.copy(workspace_root / "ci/maven/settings.xml", workspace_root / "server/settings.xml")
    print(f"start build  server")
    print(f"workspace_root=={workspace_root}")
    try:
        utils.do_subprocess("docker rm -f mvn-build", "clean mvn container")
    except Exception as e:
        log.error(f"clean error:{e}")
    utils.do_subprocess(
        f"docker run --add-host='devrepo.devcloud.cn-north-4.huaweicloud.com:192.168.0.5' --add-host='repo.huaweicloud.com:192.168.0.224' --name mvn-build \
            -v {workspace_root / "server"}:/workspace \
            -v {workspace_root / "repository"}:/repository \
            maven:3.8.5-openjdk-17 \
            bash /workspace/mvn_build.sh ",
        "mvn clean package")


def build_images(m_confs: list[Module], patch_databases: list[str] = None) -> list[str]:
    tags = []
    for m_conf in m_confs:
        os.chdir(workspace_root / m_conf.path)
        print(f"m_conf.path={workspace_root}/{m_conf.path}")
        match = re.search(r"SPC\d{3}B\d*", m_conf.version)
        mirror_ver = match.group() if match else ""
        tag = f"{m_conf.image_name}:{mirror_ver}.{m_conf.arch}.{LOCAL_TIME}"

        if m_conf.name == "db-migrate" and patch_databases is not None:
            build_db_migrate_patch_image(m_conf, mirror_ver, tag, patch_databases)
        else:
            utils.do_subprocess(f"docker build -f Dockerfile_{m_conf.arch} -t {tag} .", f"docker build {m_conf.name}")
        tags.append(tag)
    return tags


def build_db_migrate_patch_image(m_conf: Module, mirror_ver: str, tag: str, databases: list[str]):
    db_migrate_path = workspace_root / m_conf.path
    arch_suffix = m_conf.arch
    original_dockerfile = db_migrate_path / f"Dockerfile_{arch_suffix}"
    dynamic_dockerfile = db_migrate_path / f"Dockerfile_{arch_suffix}.dynamic"

    shutil.copy(original_dockerfile, dynamic_dockerfile)
    with open(dynamic_dockerfile, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    filtered_lines = [l for l in lines if not l.startswith("COPY ./agent/")]
    # writelines 不会自动补换行, 若原文件末行无 \n, 会与后续 append 的内容首尾相连,
    # 导致两条指令拼成一行 (实测在 arm64 Dockerfile 末尾无换行时, dbjob 镜像 build 失败).
    # 这里统一确保每行都以 \n 结尾, 再写回.
    normalized_lines = [(l if l.endswith("\n") else l + "\n") for l in filtered_lines]
    with open(dynamic_dockerfile, 'w', encoding='utf-8') as f:
        f.writelines(normalized_lines)
    with open(dynamic_dockerfile, 'a', encoding='utf-8') as f:
        for db_name in databases:
            db_dir = db_migrate_path / db_name
            if db_dir.exists() and db_dir.is_dir():
                f.write(f"COPY ./{db_name}/ /flyway/db/migration/{db_name}/\n")
        f.write("COPY ./database.txt /flyway/db/migration/database.txt\n")
        f.write("COPY ./flyway.conf /flyway/conf/flyway.conf\n")
        f.write("COPY ./docker_run.sh /usr/bin/docker_run.sh\n")

    patch_db_txt = db_migrate_path / "database.txt"
    original_db_txt_content = patch_db_txt.read_text() if patch_db_txt.exists() else ""
    try:
        patch_db_txt.write_text("\n".join(databases) + "\n")
        utils.do_subprocess(f"docker build -f {dynamic_dockerfile.name} -t {tag} .", f"docker build {m_conf.name} (patch mode)")
    finally:
        if original_db_txt_content:
            patch_db_txt.write_text(original_db_txt_content)
        elif patch_db_txt.exists():
            patch_db_txt.write_text("")
        if dynamic_dockerfile.exists():
            dynamic_dockerfile.unlink()


def save_images(tags: list[str], tmp_path: Path):
    os.chdir(tmp_path)
    opt = " ".join(tags)
    os.makedirs(tmp_path / 'images', exist_ok=True)
    utils.do_subprocess(f"docker save -o images/agent.tar {opt}", f"docker save images:{opt}")
    imgs_text = "\n".join(tags)
    with open('images.txt', 'w') as f:
        f.write(imgs_text)


def clean_images(tags: list[str]):
    opt = " ".join(tags)
    utils.do_subprocess(f"docker rmi {opt}", f"clean docker images:{opt}")

def upload_version(tmp_path: Path, r_conf: PackageConf, pkg_filename: str = None):
    print(f"start uoload  version")
    daily_path = f"Daily/{date_part}/Agent/{r_conf.codeBranch}/{r_conf.arch}/{now}"
    if pkg_filename is None:
        agent_file = f"{tmp_path}/eAgent_{r_conf.version}_{r_conf.arch}.zip"
    else:
        agent_file = f"{tmp_path}/{pkg_filename}"
    cmd = f'curl --header "Authorization: Bearer {r_conf.gitee_api_token}" -T {agent_file} "{r_conf.gitee_repo}/{daily_path}/"'
    print(f"cmd= {cmd}")
    utils.do_subprocess(cmd, f"上传文件到 {daily_path}")

def copy_folder(src: Path, dst: Path):
    if dst.exists():
        shutil.rmtree(dst)  # 如果目标存在，先删除
    shutil.copytree(src, dst)
    print(f"copy {src} to {dst}")


def copy_k8s_template(tmp_path: Path, r_conf: PackageConf, patch_auxiliaries: list[str] = None):
    now = datetime.now().strftime("%Y%m%d%H%M%S")
    arch_tag = f"{r_conf.arch}.{LOCAL_TIME}"
    match = re.search(r"SPC\d{3}B\d*", r_conf.version)
    mirror_ver = match.group() if match else ""

    target_dirs = [tmp_path / 'conf' / 'manifests', tmp_path / 'init', tmp_path / 'conf' / 'ext', tmp_path / 'conf' / 'pv', tmp_path / 'conf' / 'job']

    for t_path in target_dirs:
        if not t_path.exists():
            continue
        for file in t_path.iterdir():
            with open(file, 'r+', encoding='utf-8') as f:
                content = f.read()
                content = content.replace("{{ .Values.version }}", mirror_ver)
                content = content.replace("{{ .Values.arch }}", arch_tag)
                content = content.replace("{{ .Values.now }}", now)
            with open(file, 'w', encoding='utf-8') as f:
                f.write(content)


def create_app_info(tmp_path: Path, r_conf: PackageConf):
    if (script_root / "app-info.yaml").exists():
        shutil.copy(script_root / "app-info.yaml", tmp_path / "app-info.yaml")

def recreate_tmp() -> Path:
    tmp_path = workspace_root / "dist"
    # 删除已存在的 tmp 目录及其内容
    if tmp_path.exists():
        shutil.rmtree(tmp_path)
        # 重新创建 tmp 目录
    tmp_path.mkdir(parents=True, exist_ok=False)
    return tmp_path


def copy_docs(tmp_path: Path):
    if (script_root / "ci_env.toml").exists():
        shutil.copy(script_root / "ci_env.toml", tmp_path / "info.toml")


def copy_backup_config(tmp_path: Path, patch_databases: list[str] = None, is_patch: bool = False):
    backup_config_src = script_root / "template" / "backup_config"
    backup_config_dst = tmp_path / "backup_config"
    os.makedirs(backup_config_dst, exist_ok=True)

    if backup_config_src.exists():
        for item in backup_config_src.iterdir():
            dst_item = backup_config_dst / item.name
            if item.is_dir():
                if dst_item.exists():
                    shutil.rmtree(dst_item)
                shutil.copytree(item, dst_item)
            else:
                shutil.copy2(item, dst_item)
        log.info(f"copy backup_config from {backup_config_src} to {backup_config_dst}")

    if is_patch:
        dbs = patch_databases if patch_databases else []
        gen_patch_backupdb_config(backup_config_dst, dbs)
        gen_patch_database_txt(backup_config_dst, dbs)
    else:
        db_migrate_backup_config = script_root / "db-migrate" / "backupdb_config.yml"
        if db_migrate_backup_config.exists():
            dst_file = backup_config_dst / "backupdb_config.yml"
            if not dst_file.exists():
                shutil.copy(db_migrate_backup_config, dst_file)
                log.info(f"copy backupdb_config.yml from {db_migrate_backup_config} to {dst_file}")

        db_migrate_database_txt = script_root / "db-migrate" / "database.txt"
        if db_migrate_database_txt.exists():
            dst_file = backup_config_dst / "database.txt"
            if not dst_file.exists():
                shutil.copy(db_migrate_database_txt, dst_file)
                log.info(f"copy database.txt from {db_migrate_database_txt} to {dst_file}")


def gen_patch_backupdb_config(dst_dir: Path, databases: list[str]):
    config_path = dst_dir / "backupdb_config.yml"
    if not databases:
        content = "db_backup:\n  enabled: false\n  databases: []\n  mongodb: []\n"
    else:
        db_entries = []
        for db_name in databases:
            db_entries.append(f'    - name: "{db_name}"\n      blacklist_tables: []\n      whitelist_tables: []')
        content = "db_backup:\n  databases:\n" + "\n".join(db_entries) + "\n"
    config_path.write_text(content, encoding='utf-8')
    log.info(f"generated patch backupdb_config.yml for databases: {databases}")


def gen_patch_database_txt(dst_dir: Path, databases: list[str]):
    if not databases:
        log.info("no databases for this patch, skip generating database.txt")
        return
    txt_path = dst_dir / "database.txt"
    txt_path.write_text("\n".join(databases) + "\n", encoding='utf-8')
    log.info(f"generated patch database.txt for databases: {databases}")


def parse_service_auxiliary_mapping() -> dict:
    mapping_file = script_root / "service_auxiliary_mapping.conf"
    if not mapping_file.exists():
        log.warning(f"mapping file not found: {mapping_file}")
        return {}
    result = {}
    with open(mapping_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split(':', 1)
            if len(parts) != 2:
                continue
            svc_name = parts[0].strip()
            rhs = parts[1].strip()
            aux_part = rhs
            databases = []
            if '|' in rhs:
                aux_part, db_part = rhs.split('|', 1)
                databases = [d.strip() for d in db_part.split(',') if d.strip()]
            auxiliaries = [a.strip() for a in aux_part.split(',') if a.strip()]
            result[svc_name] = {"auxiliaries": auxiliaries, "databases": databases}
    return result


def resolve_patch_auxiliaries(patch_services: list[str]) -> tuple[list[str], list[str]]:
    mapping = parse_service_auxiliary_mapping()
    all_auxiliaries = set()
    all_databases = set()
    for svc in patch_services:
        if svc in mapping:
            all_auxiliaries.update(mapping[svc]["auxiliaries"])
            all_databases.update(mapping[svc]["databases"])
        else:
            log.warning(f"service '{svc}' not found in mapping, skipping auxiliary resolution")
    return sorted(all_auxiliaries), sorted(all_databases)


def filter_deploy_configs(tmp_path: Path, patch_services: list[str], auxiliaries: list[str]):
    """
    在 copy_folder 之后对 tmp_path/conf 进行过滤, 只保留补丁服务相关的部署配置.
    操作 tmp_path (临时产物目录), 不动源码 template 目录, 避免污染工作区.

    规则:
    - manifests: 只保留补丁服务对应的 yaml (dbjob/mock-ai 无 manifest)
    - ext: 仅当补丁含 mock-ai 时保留, 否则清空
    - job: 仅当补丁含 flyway_job 辅助产物时保留, 否则清空

    注意: patch_services 中的名称是 package.toml 的模块名(如 crs-module-system),
    而 manifests 目录下的 yaml 文件名用的是镜像名前缀(如 agent-system.yaml).
    本函数会从 package.toml 读取 image_name, 取末段作为 manifest 文件名前缀,
    避免命名不一致导致 manifest 被误删.
    """
    conf_path = tmp_path / "conf"
    manifests_path = conf_path / "manifests"
    job_path = conf_path / "job"
    ext_path = conf_path / "ext"

    has_flyway_job = bool(auxiliaries) and "flyway_job" in auxiliaries
    has_mock_ai = "mock-ai" in patch_services

    # 构建 模块名 → manifest 文件名前缀 的映射
    # 优先用 package.toml 中 image_name 的末段(如 agent-system),
    # 因为 manifests/agent-system.yaml 用的就是这个前缀.
    # 兜底:模块名本身(适用于 manifest 文件名与模块名一致的场景)
    module_to_manifest_prefix = {}
    try:
        p = script_root / "package.toml"
        if p.exists():
            import toml as _toml
            confs = _toml.load(p)
            for m in confs.get("modules", []):
                name = m.get("name", "")
                image_name = m.get("image_name", "")
                if image_name:
                    prefix = image_name.split("/")[-1]
                    module_to_manifest_prefix[name] = prefix
                else:
                    module_to_manifest_prefix[name] = name
    except Exception as e:
        log.warning(f"failed to load package.toml for manifest prefix mapping: {e}")

    manifest_keep = []
    for svc in patch_services:
        if svc in ("dbjob", "mock-ai"):
            continue
        # 优先用映射后的 manifest 前缀,兜底用 svc 本身
        prefix = module_to_manifest_prefix.get(svc, svc)
        manifest_keep.append(prefix)
        if prefix != svc:
            log.info(f"manifest prefix mapping: {svc} -> {prefix}")

    if manifests_path.exists():
        if not manifest_keep:
            for item in manifests_path.iterdir():
                if item.is_dir():
                    shutil.rmtree(item)
                else:
                    item.unlink()
            log.info("no manifest to keep, cleared manifests")
        else:
            keep_pattern = "|".join(re.escape(s) for s in manifest_keep)
            for item in list(manifests_path.iterdir()):
                if not re.search(keep_pattern, item.name):
                    if item.is_dir():
                        shutil.rmtree(item)
                    else:
                        item.unlink()
            log.info(f"kept manifests: {manifest_keep}")

    if ext_path.exists():
        if not has_mock_ai:
            for item in ext_path.iterdir():
                if item.is_dir():
                    shutil.rmtree(item)
                else:
                    item.unlink()
            log.info("cleared ext")
        else:
            log.info("kept ext (mock-ai)")

    if job_path.exists():
        # job 目录精细化过滤:
        # - dbjob.yaml:仅当 has_flyway_job 时保留(flyway 数据库迁移)
        # - job-appinfo.yaml:始终保留(每次升级都要刷新版本号 + 触发菜单刷新)
        # - intermediary.yaml:始终清空(服务注册,首次安装专属,补丁模式不应重跑)
        # - job-route.yaml:始终清空(APISIX 路由配置,首次安装专属,补丁模式不应重跑)
        # - 其他未识别的 job:仅当 has_flyway_job 时保留(向后兼容)
        JOB_KEEP_ALWAYS = {"job-appinfo.yaml"}
        JOB_KEEP_ONLY_FLYWAY = {"dbjob.yaml"}
        JOB_DROP_ALWAYS = {"intermediary.yaml", "job-route.yaml"}

        kept_jobs = []
        for item in list(job_path.iterdir()):
            if item.is_dir():
                shutil.rmtree(item)
                continue
            name = item.name
            if name in JOB_DROP_ALWAYS:
                item.unlink()
                log.info(f"dropped job (first-install only): {name}")
            elif name in JOB_KEEP_ALWAYS:
                kept_jobs.append(name)
            elif name in JOB_KEEP_ONLY_FLYWAY:
                if has_flyway_job:
                    kept_jobs.append(name)
                else:
                    item.unlink()
                    log.info(f"dropped job (no flyway_job): {name}")
            else:
                if has_flyway_job:
                    kept_jobs.append(name)
                else:
                    item.unlink()
                    log.info(f"dropped job (unknown, no flyway_job): {name}")
        log.info(f"kept jobs: {kept_jobs}")

    log.info("filter deploy configs completed")


def resolve_deployment_name(image_tag: str, tmp_path: Path) -> str:
    """
    从补丁包 tmp_path/conf/manifests 的 yaml 中解析真实的 K8s Deployment 名称,
    避免依赖 image_name 末段等于 deployment 名的假设.

    匹配规则: 找到 kind: Deployment 块中 metadata.name 字段.
    若解析失败, 回退到 image_tag 的 image_name 末段 (保持向后兼容).
    """
    image_name = image_tag.split("/")[1].split(":")[0]
    manifests_path = tmp_path / "conf" / "manifests"
    if manifests_path.exists():
        for item in manifests_path.iterdir():
            if not item.is_file() or item.suffix not in (".yaml", ".yml"):
                continue
            try:
                docs = list(yaml.safe_load_all(item.read_text(encoding="utf-8")))
            except Exception as e:
                log.warning(f"parse manifest {item} failed: {e}")
                continue
            for doc in docs:
                if not isinstance(doc, dict):
                    continue
                if doc.get("kind") != "Deployment":
                    continue
                containers = (doc.get("spec", {}) or {}).get("template", {}).get("spec", {}).get("containers", [])
                for c in containers:
                    c_image = (c.get("image") or "")
                    if image_name in c_image:
                        meta_name = (doc.get("metadata", {}) or {}).get("name")
                        if meta_name:
                            log.info(f"resolved deployment name '{meta_name}' from {item.name} for image {image_name}")
                            return meta_name
    log.warning(f"cannot resolve deployment name from manifests for {image_name}, fallback to image_name tail")
    return image_name


def create_sha512(packages_path: Path):
    """
    计算packages_path目录下所有文件的sha512校验和进行签名处理
    """
    signconf_cms_shell=script_root / "create_signconf_cms.sh"
    cmd = f'{signconf_cms_shell} {packages_path}'
    # 切换到packages_path目录
    os.chdir(packages_path)

    # 调用create_signconf_cms.sh脚本
    utils.do_subprocess(cmd, f"版本包签名")
    
def create_snapshot(packages_path: Path):
    # 切换到create_snapshot_path目录
    create_snapshot_path=script_root / "snapshots/linux"
    os.chdir(create_snapshot_path)

    for key in ["password", "gitee_repo", "gitee_api_token", "gitee_backup"]:
        os.environ.pop(key, None)

    # 调用create_signconf_cms.sh脚本
    utils.do_subprocess(f"ant -f build.xml", f"制作环境快照")
    shutil.copy(create_snapshot_path / "result/snapshot.htm", packages_path / "snapshot.htm")