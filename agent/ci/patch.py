import argparse
import sys

from common.ssh import SshSession
from func import *


def main():
    parser = argparse.ArgumentParser(description="Agent patch package tool")
    parser.add_argument("-n", "--name", action="append", type=str, required=True,
                        help="service names to patch (e.g. -n crs-gateway -n crs-module-system)")
    parser.add_argument("-m", "--mvn", action='store_true', help="maven build or not")
    parser.add_argument("-w", "--web", action='store_true', help="build web")
    parser.add_argument("-s", "--scp", type=str, required=False,
                        default=None, help="scp host for direct deploy")
    parser.add_argument("--port", type=str, required=False,
                        default=22, help="scp port")
    parser.add_argument("-u", "--user", type=str, required=False,
                        default=None, help="scp user")
    parser.add_argument("-p", "--password", type=str, required=False,
                        default=None, help="scp password")
    parser.add_argument("--package", action='store_true',
                        help="generate patch package zip instead of direct SCP deploy")
    args = parser.parse_args()
    log.info(f"patch args: {args}")

    patch_id = read_patch_id()
    if not patch_id:
        log.error("cannot build patch package without version_patch_id, abort")
        sys.exit(1)
    log.info(f"patch_id: {patch_id}")

    if args.mvn:
        maven_package()

    if args.web:
        build_web()

    r_confs = read_conf()

    patch_services = args.name
    log.info(f"patch target services: {patch_services}")

    auxiliaries, databases = resolve_patch_auxiliaries(patch_services)
    log.info(f"resolved auxiliaries: {auxiliaries}")
    log.info(f"resolved databases: {databases}")

    mos = []
    for m in r_confs.modules:
        if m.name in patch_services:
            mos.append(m)
    if "flyway_job" in auxiliaries:
        for m in r_confs.modules:
            if m.name == "db-migrate" and m not in mos:
                mos.append(m)
                break
    log.info(f"modules to build: {[i.name for i in mos]}")
    if len(mos) == 0:
        log.error("no modules found for patch, exiting")
        return

    patch_databases = databases if databases else None
    build_tags = build_images(mos, patch_databases=patch_databases)

    tmp_path = recreate_tmp()
    copy_folder(script_root / "template", tmp_path)

    # 在 copy_folder 之后过滤 tmp_path/conf, 只保留补丁服务的部署配置 (不动源码 template 目录)
    filter_deploy_configs(tmp_path, patch_services, auxiliaries)

    copy_k8s_template(tmp_path, r_confs, patch_auxiliaries=auxiliaries)
    create_app_info(tmp_path, r_confs)
    copy_backup_config(tmp_path, patch_databases=patch_databases, is_patch=True)
    save_images(build_tags + [f"{ti}{r_confs.arch}" for ti in r_confs.third_imgs_prefix], tmp_path)
    copy_docs(tmp_path)
    create_snapshot(tmp_path)
    create_sha512(tmp_path)

    if args.package:
        os.chdir(tmp_path)
        pkg_name = f"eAgent_patch_{patch_id}_{r_confs.arch}.zip"
        cmd_runner = f"zip -5 -r {pkg_name} ."
        utils.do_subprocess(cmd_runner, f"zip patch package")
        upload_version(tmp_path, r_confs, pkg_name)
    elif args.scp is not None and args.user is not None and args.password is not None:
        session = SshSession(args.scp, args.user, args.password, args.port)
        session.connect()
        os.chdir(tmp_path)
        session.put(tmp_path / "images" / "agent.tar", "/home/agent.tar")
        session.exec_command("docker load -i /home/agent.tar")
        for tag in build_tags:
            deployment_name = resolve_deployment_name(tag, tmp_path)
            log.info(f"kubectl set image deployment/{deployment_name} -n agent {deployment_name}={tag}")
            session.exec_command(
                f"kubectl set image deployment/{deployment_name} -n agent {deployment_name}={tag}")
        session.exec_command("rm -f /home/agent.tar")
    else:
        os.chdir(tmp_path)
        pkg_name = f"eAgent_patch_{patch_id}_{r_confs.arch}.zip"
        cmd_runner = f"zip -5 -r {pkg_name} ."
        utils.do_subprocess(cmd_runner, f"zip patch package")

    clean_images(build_tags)


if __name__ == '__main__':
    main()