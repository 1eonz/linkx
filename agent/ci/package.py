from func import *


def main():
    r_confs = read_conf()
    maven_package()
    build_web()
    build_tags = build_images(r_confs.modules)
    tmp_path = recreate_tmp()
    copy_folder(script_root / "template", tmp_path)
    copy_k8s_template(tmp_path, r_confs)
    create_app_info(tmp_path, r_confs)
    copy_backup_config(tmp_path)
    save_images(build_tags + [f"{ti}{r_confs.arch}" for ti in r_confs.third_imgs_prefix], tmp_path)
    copy_docs(tmp_path)
    create_snapshot(tmp_path)
    create_sha512(tmp_path)
    os.chdir(tmp_path)
    cmd_runner=f"zip -5 -r eAgent_{r_confs.version}_{r_confs.arch}.zip ."
    utils.do_subprocess(cmd_runner, f"tar")
    upload_version(tmp_path, r_confs)
    clean_images(build_tags)


if __name__ == '__main__':
    main()