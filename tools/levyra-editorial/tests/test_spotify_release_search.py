from levyra_editorial.spotify import select_official_spotify_playlist_id


def playlist(identifier: str, name: str, owner: str = "spotify") -> dict:
    return {
        "id": identifier,
        "name": name,
        "owner": {"id": owner, "display_name": owner.title()},
    }


def test_selector_prefers_exact_local_spotify_playlist() -> None:
    items = [
        playlist("userplaylist000000000001", "New Music Friday Italia", owner="someone"),
        playlist("globalplaylist0000000001", "New Music Friday"),
        playlist("italiaplaylist0000000001", "New Music Friday Italia"),
    ]

    assert (
        select_official_spotify_playlist_id(
            items,
            "New Music Friday Italia",
            ["New Music Friday Italia"],
        )
        == "italiaplaylist0000000001"
    )


def test_selector_rejects_non_spotify_owner() -> None:
    assert (
        select_official_spotify_playlist_id(
            [playlist("userplaylist000000000001", "New Music Friday Italia", owner="user")],
            "New Music Friday Italia",
        )
        is None
    )
